package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.type.constraints.TypeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeAnnotations;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ContainableProcessor extends TypeImplementationProcessor {
  private static final int CONTAINABLE_SUPERTYPE=450;
  private static final int CONSTRUCTOR_UNIQUE_RETURN = 451;
  private static final int CONSTRUCTOR_BORROWED_RECEVIER = 452;
  private static final int CONSTRUCTOR_BAD = 453;
  private static final int METHOD_BORROWED_RECEIVER = 454;
  private static final int METHOD_BAD = 455;
  private static final int FIELD_CONTAINED_PRIMITIVE = 456;
  private static final int FIELD_CONTAINED_VOUCHED = 457;
  private static final int FIELD_CONTAINED_VOUCHED_WITH_REASON = 458;
  private static final int FIELD_CONTAINED_OBJECT = 459;
  private static final int FIELD_BAD = 460;
  private static final int FIELD_IS_UNIQUE = 461;
  private static final int FIELD_IS_NOT_UNIQUE = 462;
  private static final int OBJECT_IS_CONTAINABLE = 463;
  private static final int OBJECT_IS_NOT_CONTAINABLE = 464;
  private static final int TYPE_IS_CONTAINABLE = 465;
  private static final int TYPE_IS_NOT_CONTAINABLE = 466;
  private static final int CONTAINABLE_IMPL = 467;
  private static final int TRIVIALLY_CONTAINABLE_EMPTY = 468;
  private static final int TRIVIALLY_CONTAINABLE_STATIC_FIELDS = 469;
  
  
  
  private final boolean isInterface;
  private final ResultsBuilder builder;
  private boolean hasMethods = false; // only read when isInterface is true
  private boolean hasStaticFields = false; // only read when isInterface is true
  
  
  
  public ContainableProcessor(final IBinder b,
			final ContainablePromiseDrop cDrop,
			final IRNode typeDecl, final IRNode typeBody) {
		super(b, typeDecl, typeBody);
		isInterface = InterfaceDeclaration.prototype.includes(typeDecl);
		builder = new ResultsBuilder(cDrop);
	}


  
  @Override
  protected void postProcess() {
    // We are only called on classes annotated with @Immutable
    if (isInterface) {
      if (!hasMethods) {
        if (!hasStaticFields) {
          builder.createRootResult(true, typeDecl, TRIVIALLY_CONTAINABLE_EMPTY);
        } else {
          builder.createRootResult(
              true, typeDecl, TRIVIALLY_CONTAINABLE_STATIC_FIELDS);
        }
      }
    }
  }

	@Override
	protected void processSuperType(final IRNode name, final IRNode tdecl) {
	  final ContainablePromiseDrop pDrop =
		  LockRules.getContainableImplementation(tdecl);
	  if (pDrop != null) {
  		final ResultDrop result = builder.createRootResult(
  		    true, name, CONTAINABLE_SUPERTYPE,
  		    JavaNames.getQualifiedTypeName(tdecl));
  		result.addTrusted(pDrop);
	  }
	}
	
	@Override
	protected void processConstructorDeclaration(final IRNode cdecl) {
		final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
		final BorrowedPromiseDrop bpd = UniquenessRules
				.getBorrowed(rcvrDecl);

		final IRNode returnDecl = JavaPromise.getReturnNodeOrNull(cdecl);
		final UniquePromiseDrop upd = UniquenessRules.getUnique(returnDecl);

		// Prefer unique return over borrowed receiver
		final String id = JavaNames.genSimpleMethodConstructorName(cdecl);
		if (upd != null) {
			final ResultDrop result = builder.createRootResult(
			    true, cdecl, CONSTRUCTOR_UNIQUE_RETURN, id);
			result.addTrusted(upd);
		} else if (bpd != null) {
			final ResultDrop result = builder.createRootResult(
			    true, cdecl, CONSTRUCTOR_BORROWED_RECEVIER, id);
			result.addTrusted(bpd);
		} else {
			final ResultDrop result = builder.createRootResult(
			    false, cdecl, CONSTRUCTOR_BAD, id);
			result.addProposal(new ProposedPromiseDrop(
			    "Unique", "return", cdecl, cdecl, Origin.MODEL));
		}
	}

	@Override
	protected void processMethodDeclaration(final IRNode mdecl) {
	  hasMethods = true;
	  
		// Must borrow the receiver if the method is not static
		if (!TypeUtil.isStatic(mdecl)) {
			final String id = JavaNames.genSimpleMethodConstructorName(mdecl);
			final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(mdecl);
			final BorrowedPromiseDrop bpd = UniquenessRules.getBorrowed(rcvrDecl);
			if (bpd == null) {
				final ResultDrop result = builder.createRootResult(
				    false, mdecl, METHOD_BAD, id);
				result.addProposal(new ProposedPromiseDrop(
				    "Borrowed",	"this", mdecl, mdecl, Origin.MODEL));
			} else {
				final ResultDrop result = builder.createRootResult(
				    true, mdecl, METHOD_BORROWED_RECEIVER, id);
				result.addTrusted(bpd);
			}
		}
	}

	@Override
	protected void processVariableDeclarator(final IRNode fieldDecl,
			final IRNode varDecl, final boolean isStatic) {
	  if (!isStatic) {
  	  assureFieldIsContainable(fieldDecl, varDecl);
	  } else {
	    hasStaticFields = true;
	  }
	}

	private void assureFieldIsContainable(
	    final IRNode fieldDecl, final IRNode varDecl) {
	    final String id = VariableDeclarator.getId(varDecl);
	    final IJavaType type = binder.getJavaType(varDecl);
    if (type instanceof IJavaPrimitiveType) {
      builder.createRootResult(
          true, varDecl, FIELD_CONTAINED_PRIMITIVE, id);
    } else {
      final VouchFieldIsPromiseDrop vouchDrop = LockRules
          .getVouchFieldIs(varDecl);
      if (vouchDrop != null && vouchDrop.isContainable()) {
        final String reason = vouchDrop.getReason();
        final ResultDrop result = (reason == VouchFieldIsNode.NO_REASON)
            ? builder.createRootResult(true, varDecl, FIELD_CONTAINED_VOUCHED, id)
            : builder.createRootResult(true, varDecl, FIELD_CONTAINED_VOUCHED_WITH_REASON, id, reason);
        result.addTrusted(vouchDrop);
      } else {
        /* Use a result folder: We have two things that need to be true:
         * (1) The type of the field is @Containable
         * (2) The field is @Unique
         */       
        final ResultFolderDrop folder = builder.createRootAndFolder(
            varDecl, FIELD_CONTAINED_OBJECT, FIELD_BAD, id);
        
        final IUniquePromise uniqueDrop = UniquenessUtils.getUnique(varDecl);
        final ResultDrop uResult = ResultsBuilder.createResult(
            folder, varDecl, uniqueDrop != null,
            FIELD_IS_UNIQUE, FIELD_IS_NOT_UNIQUE);
        if (uniqueDrop != null) {
          uResult.addTrusted(uniqueDrop.getDrop());
        } else {
          uResult.addProposal(new ProposedPromiseDrop(
              "Unique", null, varDecl, varDecl, Origin.MODEL));
        }

        final ResultFolderDrop typeFolder = ResultsBuilder.createOrFolder(
            folder, varDecl, OBJECT_IS_CONTAINABLE, OBJECT_IS_NOT_CONTAINABLE);

        final TypeAnnotationTester tester = 
            new TypeAnnotationTester(TypeAnnotations.CONTAINABLE, binder,
                ParameterizedTypeAnalysis.getFolders());
        final IRNode typeDeclNode = FieldDeclaration.getType(fieldDecl);
        final boolean isContainable = tester.testFieldDeclarationType(typeDeclNode);
        final ResultDrop cResult = ResultsBuilder.createResult(typeFolder, typeDeclNode,
            isContainable, TYPE_IS_CONTAINABLE, TYPE_IS_NOT_CONTAINABLE,
            type.toSourceText());
        cResult.addTrusted(tester.getTrusts());
        
        boolean proposeContainable = !isContainable;
        if (TypeUtil.isFinal(varDecl) && !isContainable) {
          /*
           * If the type is not containable, we can check to see
           * if the implementation assigned to the field is containable,
           * but only if the field is final.
           */
          final IRNode init = VariableDeclarator.getInit(varDecl);
          if (Initialization.prototype.includes(init)) {
            final IRNode initExpr = Initialization.getValue(init);
            if (NewExpression.prototype.includes(initExpr)) {
              final TypeAnnotationTester tester2 =
                  new TypeAnnotationTester(TypeAnnotations.CONTAINABLE, binder,
                      ParameterizedTypeAnalysis.getFolders());
              if (tester2.testFinalObjectType(NewExpression.getType(initExpr))) {
                // we have an instance of an immutable implementation
                proposeContainable = false;
                final ResultDrop result = ResultsBuilder.createResult(
                    true, typeFolder, initExpr, CONTAINABLE_IMPL);
                result.addTrusted(tester2.getTrusts());
              }
            }
          }
        }
        
        if (proposeContainable) {
          for (final IRNode t : tester.getFailed()) {
            cResult.addProposal(new ProposedPromiseDrop(
                "Containable", null, t, varDecl, Origin.MODEL));
          }
        }
      
        folder.addProposalNotProvedConsistent(new ProposedPromiseDrop(
            "Vouch", "Containable", varDecl, varDecl, Origin.MODEL));
      }
    }
  }
}
