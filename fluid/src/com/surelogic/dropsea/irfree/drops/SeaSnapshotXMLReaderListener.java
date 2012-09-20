package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.AND_TRUSTED_PROOF_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNO_ATTRS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_BY_RESULTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_PROMISE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DEPENDENT_PROMISES;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DEPONENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DEPONENT_PROMISES;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FLAVOR_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_REF;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FULL_TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_ABOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_LABEL;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_TRUSTED_PROMISE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_TRUSTED_PROOF_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.RESULT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.SUB_FOLDER;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TRUSTED_FOLDER;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TRUSTED_PROMISE;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.JAVA_DECL_INFO;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.PROPERTIES;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.SOURCE_REF;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.common.xml.AbstractXMLResultListener;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.AnalysisHintDrop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.ScopedPromiseDrop;
import com.surelogic.dropsea.irfree.DropTypeUtility;

public final class SeaSnapshotXMLReaderListener extends AbstractXMLResultListener {

  private class SeaEntity extends Entity {

    public SeaEntity(String name, Attributes a) {
      super(name, a);
    }

    IRFreeDrop f_drop;

    void setDrop(IRFreeDrop drop) {
      f_drop = drop;
    }

    IRFreeDrop getDrop() {
      return f_drop;
    }

    /**
     * Special processing for proposed promises.
     */
    @Override
    public void addRef(Entity e) {
      if (f_drop instanceof IRFreeProposedPromiseDrop) {
        IRFreeProposedPromiseDrop ppd = (IRFreeProposedPromiseDrop) f_drop;
        // System.out.println("-- addRef ON A PROPOSED PROMISE DROP CALLED");
        final String name = e.getName();
        if (SOURCE_REF.equals(name)) {
          SourceRef sr = new SourceRef(e);
          if (FROM_REF.equals(e.getAttribute(FLAVOR_ATTR))) {
            ppd.setAssumptionRef(IRFreeDrop.makeSrcRef(sr));
          } else {
            setSource(sr);
          }
        } else if (PROPERTIES.equals(name)) {
          if (ANNO_ATTRS.equals(e.getAttribute(FLAVOR_ATTR))) {
            ppd.setAnnoAttributes(e.getAttributes());
          } else {
            ppd.setReplacedAttributes(e.getAttributes());
          }
        } else
          super.addRef(e);
      } else
        super.addRef(e);
    }
  }

  void handleJavaDecl(Entity e, JavaDeclInfo info) {
    if (e instanceof SeaEntity) {
      final IRFreeDrop drop = ((SeaEntity) e).getDrop();
      if (drop instanceof IRFreeProposedPromiseDrop) {
        final IRFreeProposedPromiseDrop ppd = (IRFreeProposedPromiseDrop) drop;
        ppd.addInfo(info);
      }
    }
  }

  /**
   * The index in the list matches the entity's id.
   */
  private final ArrayList<Entity> entities = new ArrayList<Entity>();

  public List<IDrop> getDrops() {
    final ArrayList<IDrop> result = new ArrayList<IDrop>();
    for (Entity se : entities) {
      if (se instanceof SeaEntity) {
        IRFreeDrop drop = ((SeaEntity) se).getDrop();
        if (drop != null)
          result.add(drop);
      }
    }
    return result;
  }

  private void add(int id, Entity info) {
    if (id >= entities.size()) {
      // Need to expand the elements in the list
      while (id > entities.size()) {
        entities.add(null);
      }
      entities.add(info);
    } else {
      Entity old = entities.set(id, info);
      if (old != null) {
        throw new IllegalStateException("Replacing id: " + id);
      }
    }
  }

  public Entity makeEntity(String name, Attributes a) {
    if (JAVA_DECL_INFO.equals(name))
      return new JavaDeclInfo(name, a);

    final SeaEntity entity = new SeaEntity(name, a);
    final String type = Entity.getValue(a, FULL_TYPE_ATTR);
    if (type != null) {
      final Class<?> thisType = DropTypeUtility.findType(type);
      if (thisType != null) {
        if (ProposedPromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeProposedPromiseDrop(entity, thisType));
        } else if (ScopedPromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeScopedPromiseDrop(entity, thisType));
        } else if (PromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreePromiseDrop(entity, thisType));
        } else if (AnalysisHintDrop.class.isAssignableFrom(thisType)) {
          /*
           * The old scheme used WarningDrop as a subtype of InfoDrop. The new
           * scheme just has AnalysisHintDrop with an attribute, hint type. We
           * need to set the hint type attribute correctly if we are dealing
           * with an old scan. No extra work for InfoDrop (default is
           * SUGGESTION), but we need to explicitly set the hint type to WARNING
           * if an old WarningDrop.
           */
          if (type.endsWith("WarningDrop"))
            entity.setDrop(new IRFreeAnalysisHintDrop(entity, thisType, IAnalysisHintDrop.HintType.WARNING));
          else
            entity.setDrop(new IRFreeAnalysisHintDrop(entity, thisType));
        } else if (ResultDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeResultDrop(entity, thisType));
        } else if (ResultFolderDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeResultFolderDrop(entity, thisType));
        } else if (ModelingProblemDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeModelingProblemDrop(entity, thisType));
        }
      }
    }
    return entity;
  }

  @Override
  protected boolean define(final int id, Entity e) {
    add(id, e);
    if (e instanceof SeaEntity) {
      /*
       * Add the source ref and supporting information (if any) to the drop
       * under construction.
       */
      IRFreeDrop drop = ((SeaEntity) e).getDrop();
      if (drop != null) {
        final SourceRef sr = e.getSource();
        if (sr != null)
          drop.setSrcRef(IRFreeDrop.makeSrcRef(sr));

        for (MoreInfo i : e.getInfos()) {
          drop.addSupportingInformation(IRFreeDrop.makeSupportingInfo(i));
        }
      }
    }
    return true;
  }

  @Override
  protected void handleRef(String fromLabel, int fromId, Entity to) {
    final String refType = to.getName();
    final Entity rawFromE = entities.get(fromId);
    final int toId = Integer.valueOf(to.getId());
    final Entity rawToE = entities.get(toId);

    IRFreeDrop fromE = null;
    if (rawFromE instanceof SeaEntity) {
      fromE = ((SeaEntity) rawFromE).getDrop();
    }
    IRFreeDrop toE = null;
    if (rawToE instanceof SeaEntity) {
      toE = ((SeaEntity) rawToE).getDrop();
    }

    if (fromE != null && toE != null) {

      /*
       * The approach is to check the types and also the XML label. If
       * everything matches a reference is set on the IRFreeDrop involved and we
       * return immediately. If we fall through all of them we throw an
       * exception that we didn't handle the link.
       */

      if (PROPOSED_PROMISE.equals(refType)) {
        /*
         * To a PROPOSED PROMISE
         */
        if (toE instanceof IRFreeProposedPromiseDrop) {
          final IRFreeProposedPromiseDrop toPPD = (IRFreeProposedPromiseDrop) toE;
          fromE.addProposal(toPPD);
          return;
        }
      }

      if (fromE instanceof IRFreeProofDrop) {
        final IRFreeProofDrop fromPD = (IRFreeProofDrop) fromE;
        /*
         * PROOF DROP
         */
        if (toE instanceof IRFreeAnalysisHintDrop) {
          final IRFreeAnalysisHintDrop toAHD = (IRFreeAnalysisHintDrop) toE;
          if (HINT_ABOUT.equals(refType)) {
            fromPD.addAnalysisHint(toAHD);
            return;
          }
        }
      }
      /*
       * Backwards compatibility with old scans to add analysis hints to promise
       * drops using only deponent links.
       */
      if (DEPONENT.equals(refType)) {
        if (fromE instanceof IRFreeAnalysisHintDrop) {
          final IRFreeAnalysisHintDrop fromAHD = (IRFreeAnalysisHintDrop) fromE;
          if (toE instanceof IRFreeProofDrop) {
            final IRFreeProofDrop toPD = (IRFreeProofDrop) toE;
            toPD.addAnalysisHint(fromAHD);
            return;
          }
        }
      }

      if (fromE instanceof IRFreePromiseDrop) {
        final IRFreePromiseDrop fromPD = (IRFreePromiseDrop) fromE;
        /*
         * PROMISE DROP
         */
        if (toE instanceof IRFreeAnalysisResultDrop) {
          final IRFreeAnalysisResultDrop toARD = (IRFreeAnalysisResultDrop) toE;
          if (CHECKED_BY_RESULTS.equals(refType)) {
            fromPD.addCheckedByResult(toARD);
            return;
          }
        } else if (toE instanceof IRFreePromiseDrop) {
          final IRFreePromiseDrop toPD = (IRFreePromiseDrop) toE;
          if (DEPENDENT_PROMISES.equals(refType)) {
            fromPD.addDependentPromise(toPD);
            return;
          } else if (DEPONENT_PROMISES.equals(refType)) {
            fromPD.addDeponentPromise(toPD);
            return;
          } else if (DEPONENT.equals(refType)) {
            /*
             * Backwards compatibility with old scans to add deponent and
             * dependent promises to promise drops using only deponent links
             */
            fromPD.addDeponentPromise(toPD);
            toPD.addDependentPromise(fromPD);
            return;
          }
        }
      }

      if (fromE instanceof IRFreeAnalysisResultDrop) {
        final IRFreeAnalysisResultDrop fromARD = (IRFreeAnalysisResultDrop) fromE;
        /*
         * ANALYSIS RESULT DROP
         */
        if (toE instanceof IRFreePromiseDrop) {
          final IRFreePromiseDrop toPD = (IRFreePromiseDrop) toE;

          if (CHECKED_PROMISE.equals(refType)) {
            fromARD.addCheckedPromise(toPD);
            return;
          }
        }
      }

      if (fromE instanceof IRFreeResultDrop) {
        final IRFreeResultDrop fromRD = (IRFreeResultDrop) fromE;
        /*
         * RESULT DROP
         */
        if (toE instanceof IRFreeProofDrop) {
          final IRFreeProofDrop toPD = (IRFreeProofDrop) toE;
          if (AND_TRUSTED_PROOF_DROP.equals(refType) || TRUSTED_FOLDER.equals(refType) || TRUSTED_PROMISE.equals(refType)) {
            fromRD.addTrusted_and(toPD);
            return;
          } else if (OR_TRUSTED_PROOF_DROP.equals(refType) || OR_TRUSTED_PROMISE.equals(refType)) {
            final String label = to.getAttribute(OR_LABEL);
            fromRD.addTrusted_or(label, toPD);
            return;
          }
        }
      }

      if (fromE instanceof IRFreeResultFolderDrop) {
        final IRFreeResultFolderDrop fromRFD = (IRFreeResultFolderDrop) fromE;
        /*
         * RESULT FOLDER DROP
         */
        if (toE instanceof IRFreeResultDrop) {
          final IRFreeResultDrop toRD = (IRFreeResultDrop) toE;
          if (RESULT.equals(refType)) {
            fromRFD.addResult(toRD);
            return;
          }
        }
        if (toE instanceof IRFreeResultFolderDrop) {
          final IRFreeResultFolderDrop toRFD = (IRFreeResultFolderDrop) toE;
          if (SUB_FOLDER.equals(refType)) {
            fromRFD.addSubFolder(toRFD);
            return;
          }
        }
      }

      /*
       * Backwards compatibility with old scans -- we use to track all proof
       * maintenance connections even though we didn't need them. We can safely
       * drop these on the floor because if the connection was useful it was
       * handled above.
       */
      if (DEPONENT.equals(refType))
        return;

      /*
       * The reference not handled if we got to here.
       */
      throw new IllegalStateException(I18N.err(248, refType, fromLabel, to.getId()));
    }
  }
}
