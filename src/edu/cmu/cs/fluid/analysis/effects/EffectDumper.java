package edu.cmu.cs.fluid.analysis.effects;

import java.text.Collator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Analysis routine to perform <I>Greenhouse</I> Java lock policy assurance.
 */
public final class EffectDumper extends AbstractWholeIRAnalysisModule 
{
  private static final Category ED_CATEGORY = Category.getInstance("EffectDumperCategory");

  static private class ResultsDepDrop extends Drop {
    // Place holder class
  }
  private Drop resultDependUpon = null;

  private IBinder binder;
  private Effects effects;

  private static EffectDumper INSTANCE;

  /**
   * Provides a reference to the sole object of this class.
   * 
   * @return a reference to the only object of this class
   */
  public static IAnalysis getInstance() {
    return INSTANCE;
  }

  /**
   * Public constructor that will be called by Eclipse when this analysis
   * module is created.
   */
  public EffectDumper() {
	super(ParserNeed.EITHER);
    INSTANCE = this;
  }

  @Override
  protected void constructIRAnalysis() {
    // FIX temporary -- should be in super class
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        binder = Eclipse.getDefault().getTypeEnv(getProject()).getBinder();
        effects = new Effects(binder, new BindingContextAnalysis(binder, true));
      }
    });    
  }

  /**
   * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeBegin(org.eclipse.core.resources.IProject)
   */
  @Override
  public void analyzeBegin(IProject project) {
    super.analyzeBegin(project);

    if (resultDependUpon != null) {
      resultDependUpon.invalidate();
      resultDependUpon = new ResultsDepDrop();
    } else {
      resultDependUpon = new ResultsDepDrop();
    }
  }

  private void setLockResultDep(IRReferenceDrop drop, IRNode node) {
    drop.setNode(node);
    if (resultDependUpon != null && resultDependUpon.isValid()) {
      resultDependUpon.addDependent(drop);
    }
  }
  
  @Override
  protected boolean doAnalysisOnAFile(final IRNode compUnit, IAnalysisMonitor monitor) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        dumpEffectsForFile(compUnit); 
      }
    });
    return true;
  }

  private void dumpEffectsForFile(final IRNode compUnit) {
    /* Run around the tree looking for method and constructor
     * declarations.  This will catch declarations inside of
     * inner classes and anonymous classes as well, which is 
     * good, because we used to miss those.
     */
    final Iterator<IRNode> nodes = JJNode.tree.topDown(compUnit);
    while (nodes.hasNext()) {
      final IRNode member = nodes.next();
      final Operator op = JJNode.tree.getOperator(member);
      final boolean isConstructor =
        ConstructorDeclaration.prototype.includes(op);
      final boolean isMethod = MethodDeclaration.prototype.includes(op);
      if (isConstructor || isMethod) {
        /* We found a method/constructor declaration. 
         * Dump the effects: (First make  sure the method is not abstract or native.)
         */
        if (!JavaNode.getModifier(member, JavaNode.ABSTRACT)
            && !JavaNode.getModifier(member, JavaNode.NATIVE)) {
          /* Can use null for the constructor context because member IS a
           * constructor or method declaration.
           */
          final Set<Effect> implFx = effects.getEffects(member, null);
          InfoDrop info = new WarningDrop();
          setLockResultDep(info, member);
          info.setMessage("Inferred: " + effectSetToString(implFx));
          info.setCategory(ED_CATEGORY);
        }
      }
    }
  }

  private String effectSetToString(final Set<Effect> set) {
    final Map<String, Effect> map = new HashMap<String, Effect>(); 
    final SortedSet<String> sortedStrings = new TreeSet<String>(Collator.getInstance());
    for (final Effect e : set) {
      final String effectString = e.toString();
      map.put(effectString, e);
      sortedStrings.add(effectString);
    }
    
    final StringBuilder sb = new StringBuilder();
    for (final Iterator<String> i = sortedStrings.iterator(); i.hasNext();) {
      final String effectString = i.next();
      sb.append(effectString.toString());
      if (map.get(effectString).isMaskable(binder)) {
        sb.append(" (Maskable)");
      }
      if (i.hasNext()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }
}