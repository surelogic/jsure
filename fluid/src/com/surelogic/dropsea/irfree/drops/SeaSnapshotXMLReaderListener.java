package com.surelogic.dropsea.irfree.drops;

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
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TRUSTED_PROOF_DROP;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.JAVA_DECL_INFO;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.PROPERTIES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.AbstractXMLResultListener;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.ir.HintDrop;
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
        if ("source-ref".equals(name)) {
          SourceRef sr = new SourceRef(e);
          if (FROM_REF.equals(e.getAttribute(FLAVOR_ATTR)) && !ppd.hasAssumptionRef()) {
            ppd.setAssumptionRef(IRFreeDrop.makeJavaRefFromSrcRef(sr));
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
      return new SeaEntity(name, a);

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
        } else if (HintDrop.class.isAssignableFrom(thisType)) {
          /*
           * The old scheme used WarningDrop as a subtype of InfoDrop. The new
           * scheme just has AnalysisHintDrop with an attribute, hint type. We
           * need to set the hint type attribute correctly if we are dealing
           * with an old scan. No extra work for InfoDrop (default is
           * SUGGESTION), but we need to explicitly set the hint type to WARNING
           * if an old WarningDrop.
           */
          if (type.endsWith("WarningDrop"))
            entity.setDrop(new IRFreeHintDrop(entity, thisType, IHintDrop.HintType.WARNING));
          else
            entity.setDrop(new IRFreeHintDrop(entity, thisType));
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
        if (!drop.hasJavaRef()) {
          final SourceRef sr = e.getSource();
          if (sr != null) {
            IRFreeDrop.makeJavaRefFromSrcRefAndAddTo(drop, sr);
          }
        }

        for (MoreInfo i : e.getInfos()) {
          /*
           * Add a hint node to replace the old-style supporting information.
           */
          IRFreeProofDrop pd = (IRFreeProofDrop) drop;
          Map<String, String> a = new HashMap<String, String>();
          a.put(AbstractXMLReader.MESSAGE_ATTR, i.message);
          a.put(AbstractXMLReader.HINT_TYPE_ATTR, IHintDrop.HintType.INFORMATION.toString());
          Entity hintE = new Entity(AbstractXMLReader.HINT_DROP, a);
          IRFreeHintDrop hint = new IRFreeHintDrop(hintE, HintDrop.class);
          if (i.source != null) {
            IRFreeDrop.makeJavaRefFromSrcRefAndAddTo(hint, i.source);
          }
          pd.addHint(hint);
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

      /*
       * To a PROPOSED PROMISE
       */
      if (PROPOSED_PROMISE.equals(refType)) {
        if (toE instanceof IRFreeProposedPromiseDrop) {
          final IRFreeProposedPromiseDrop toPPD = (IRFreeProposedPromiseDrop) toE;
          fromE.addProposal(toPPD);
          return;
        }
      }

      if (toE instanceof IRFreeHintDrop) {
        final IRFreeHintDrop toAHD = (IRFreeHintDrop) toE;
        if (HINT_ABOUT.equals(refType)) {
          fromE.addHint(toAHD);
          return;
        }
      }
      /*
       * Backwards compatibility with old scans to add analysis hints to drops
       * using only deponent links.
       */
      if (DEPONENT.equals(refType)) {
        if (fromE instanceof IRFreeHintDrop) {
          final IRFreeHintDrop fromAHD = (IRFreeHintDrop) fromE;
          toE.addHint(fromAHD);
          return;
        }
      }

      /*
       * PROMISE DROP
       */
      if (fromE instanceof IRFreePromiseDrop) {
        final IRFreePromiseDrop fromPD = (IRFreePromiseDrop) fromE;
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

      /*
       * ANALYSIS RESULT DROP
       */
      if (fromE instanceof IRFreeAnalysisResultDrop) {
        final IRFreeAnalysisResultDrop fromARD = (IRFreeAnalysisResultDrop) fromE;
        if (toE instanceof IRFreeProofDrop) {
          final IRFreeProofDrop toPD = (IRFreeProofDrop) toE;
          if (TRUSTED_PROOF_DROP.equals(refType) || "and-trusted-proof-drop".equals(refType) || "trusted-folder".equals(refType)
              || "trusted-promise".equals(refType) || "sub-folder".equals(refType) || "result".equals(refType)) {
            fromARD.addTrusted(toPD);
            return;
          } else if ("or-trusted-proof-drop".equals(refType) || "or-trusted-promise".equals(refType)) {
            // Drop on the floor -- we don't support this in old scans
            return;
          }
        }
        if (toE instanceof IRFreePromiseDrop) {
          final IRFreePromiseDrop toPD = (IRFreePromiseDrop) toE;
          if (CHECKED_PROMISE.equals(refType)) {
            fromARD.addCheckedPromise(toPD);
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
      throw new IllegalStateException(I18N.err(258, refType, fromLabel, fromE, to.getId(), toE));
    }
  }
}
