package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ANNO_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CHECKED_BY_RESULTS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CHECKED_PROMISE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DEPENDENT_PROMISES;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DEPONENT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DEPONENT_PROMISES;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FLAVOR_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DROP_TYPE_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FULL_TYPE_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.HINT_ABOUT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.JAVA_DECL_INFO;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROPERTIES;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROPOSED_PROMISE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TRUSTED_PROOF_DROP;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import org.xml.sax.Attributes;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.AbstractXmlResultListener;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.irfree.DropTypeUtility;

public final class SeaSnapshotXMLReaderListener extends AbstractXmlResultListener {
  private final ConcurrentMap<String, IJavaRef> refCache;

  // private int refsReused = 0;

  public SeaSnapshotXMLReaderListener(ConcurrentMap<String, IJavaRef> cache) {
    if (cache != null)
      refCache = cache;
    else
      refCache = null;
  }

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
        if (PROPERTIES.equals(name)) {
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

    @Override
    public IJavaRef parsePersistedRef(String encode) {
      if (refCache == null) {
        return super.parsePersistedRef(encode);
      }
      IJavaRef ref = refCache.get(encode);
      if (ref == null) {
        ref = super.parsePersistedRef(encode);
        refCache.put(encode, ref);
      } else {
        // refsReused++;
      }
      return ref;
    }
  }

  /**
   * The index in the list matches the entity's id.
   */
  private final ArrayList<Entity> entities = new ArrayList<Entity>();

  public List<IDrop> getDrops() {
    // System.out.println("Reused "+refsReused+" out of "+refCache.size());
	final Set<? extends IDrop> toFilter = computeDropsToBeFiltered();
    final ArrayList<IDrop> result = new ArrayList<IDrop>();
    for (Entity se : entities) {
      if (se instanceof SeaEntity) {
        IRFreeDrop drop = ((SeaEntity) se).getDrop();
        if (drop != null && !toFilter.contains(drop)) 
          result.add(drop);
      }
    }
    return result;
  }

  private Set<IDrop> computeDropsToBeFiltered() {
	final List<IPromiseDrop> promises = new ArrayList<IPromiseDrop>();
	final Set<IDrop> filtered = new HashSet<IDrop>();
	// Currently written to emulate ClearOutUnconnectedResultsProofHook
    for (Entity se : entities) {
    	if (se instanceof SeaEntity) {
    		IRFreeDrop drop = ((SeaEntity) se).getDrop();
    		if (drop instanceof IPromiseDrop) {
    			promises.add((IPromiseDrop) drop);
    		} 
    		else if (drop instanceof IAnalysisResultDrop) {
    			filtered.add(drop);
    		}
    	}
    }
    // Repeatedly add to connectedToAPromise until no more new drops are connected
    final Set<IProofDrop> connectedToAPromise = new HashSet<IProofDrop>();
    for(IProofDrop root : promises) {
        findConnectedResults(connectedToAPromise, filtered, root);
    }
	return filtered;
  }

  private void findConnectedResults(final Set<IProofDrop> connectedToAPromise, final Set<IDrop> filtered, IProofDrop d) {
	  if (filtered.isEmpty()) {
		  return; // Nothing to do now
	  }
	  if (connectedToAPromise.contains(d)) {
		  return; // Already handled
	  }
	  connectedToAPromise.add(d);
	  
	  if (d instanceof IPromiseDrop) {
		  IPromiseDrop pd = (IPromiseDrop) d;
		  for(IAnalysisResultDrop rd : pd.getCheckedBy()) {
			  findConnectedResults(connectedToAPromise, filtered, rd);
		  }
	  }
	  else if (d instanceof IAnalysisResultDrop) {
		  IAnalysisResultDrop rd = (IAnalysisResultDrop) d;
		  filtered.remove(d);
		  for(IProofDrop pd : rd.getTrusted()) {
			  findConnectedResults(connectedToAPromise, filtered, pd);
		  }
	  }	  
	  else throw new IllegalStateException("Unexpected proof drop: "+d);
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

  @Override
  public Entity makeEntity(String name, Attributes a) {
    if (JAVA_DECL_INFO.equals(name))
      return new SeaEntity(name, a);

    final SeaEntity entity = new SeaEntity(name, a);
    final String type = Entity.getValue(a, FULL_TYPE_ATTR);
    final String dropTypeName = Entity.getValue(a, DROP_TYPE_ATTR);
    final DropType dropType;
    if (dropTypeName != null) {
    	dropType = DropType.valueOf(dropTypeName);
    } else {
    	dropType = DropTypeUtility.computeDropType(type);
    }
    if (dropType != null) {
    	switch (dropType) {
    	case HINT:
            /*
             * The old scheme used WarningDrop as a subtype of InfoDrop. The new
             * scheme just has AnalysisHintDrop with an attribute, hint type. We
             * need to set the hint type attribute correctly if we are dealing
             * with an old scan. No extra work for InfoDrop (default is
             * SUGGESTION), but we need to explicitly set the hint type to WARNING
             * if an old WarningDrop.
             */
            if (type.endsWith("WarningDrop"))
              entity.setDrop(new IRFreeHintDrop(entity, IHintDrop.HintType.WARNING));
            else
              entity.setDrop(new IRFreeHintDrop(entity));
    		break;
    	case METRIC:
            entity.setDrop(new IRFreeMetricDrop(entity));
    		break;
    	case MODELING_PROBLEM:
            entity.setDrop(new IRFreeModelingProblemDrop(entity));
    		break;
    	case PROMISE:
            entity.setDrop(new IRFreePromiseDrop(entity));
    		break;
    	case PROPOSAL:
    		entity.setDrop(new IRFreeProposedPromiseDrop(entity));
    		break;
    	case RESULT: 
            entity.setDrop(new IRFreeResultDrop(entity));
    		break;
    	case RESULT_FOLDER:
            entity.setDrop(new IRFreeResultFolderDrop(entity));
    		break;
    	case SCOPED_PROMISE:
    		entity.setDrop(new IRFreeScopedPromiseDrop(entity));
    		break;
    	default:
    	}
    }
    else if (type != null) {
      final Class<?> thisType = DropTypeUtility.findType(type);
      if (thisType != null) {
        if (IProposedPromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeProposedPromiseDrop(entity));
        } else if (IScopedPromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeScopedPromiseDrop(entity));
        } else if (IPromiseDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreePromiseDrop(entity));
        } else if (IHintDrop.class.isAssignableFrom(thisType)) {
          /*
           * The old scheme used WarningDrop as a subtype of InfoDrop. The new
           * scheme just has AnalysisHintDrop with an attribute, hint type. We
           * need to set the hint type attribute correctly if we are dealing
           * with an old scan. No extra work for InfoDrop (default is
           * SUGGESTION), but we need to explicitly set the hint type to WARNING
           * if an old WarningDrop.
           */
          if (type.endsWith("WarningDrop"))
            entity.setDrop(new IRFreeHintDrop(entity, IHintDrop.HintType.WARNING));
          else
            entity.setDrop(new IRFreeHintDrop(entity));
        } else if (IResultDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeResultDrop(entity));
        } else if (IResultFolderDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeResultFolderDrop(entity));
        } else if (IMetricDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeMetricDrop(entity));
        } else if (IModelingProblemDrop.class.isAssignableFrom(thisType)) {
          entity.setDrop(new IRFreeModelingProblemDrop(entity));
        }
      }
    }
    return entity;
  }

  @Override
  protected boolean define(final int id, Entity e) {
    add(id, e);
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
      
      // Hack to deal with old control flow drops
      if (fromE instanceof IRFreeResultFolderDrop) {
    	  if (fromE.getMessage().startsWith("Control flow of")) {
    	      if (toE instanceof IRFreeResultDrop) {
    	    	  //System.out.println("Ignoring ref from control flow drop: "+refType);
    	    	  final IRFreeResultFolderDrop fromFD = (IRFreeResultFolderDrop) fromE;
    	    	  final IRFreeResultDrop toRD = (IRFreeResultDrop) toE;
    	    	  fromFD.addTrusted(toRD);
    	    	  return;
    	      }
    	  }
      }
      if (toE instanceof IRFreeResultFolderDrop) {
    	  if (toE.getMessage().startsWith("Control flow of")) {
    	      if (fromE instanceof IRFreeResultDrop) {
    	    	  System.out.println("Ignoring ref to control flow drop: "+refType);
    	    	  /*
    	    	  final IRFreeResultFolderDrop toFD = (IRFreeResultFolderDrop) toE;
    	    	  final IRFreeResultDrop fromRD = (IRFreeResultDrop) fromE;
    	    	  fromRD.addTrusted(toFD);    	    	   
    	    	   */
    	    	  return;
    	      }
    	  }
      }
      
      /*
       * The reference not handled if we got to here.
       */
      throw new IllegalStateException(I18N.err(258, refType, fromLabel, fromE, to.getId(), toE));
    }
  }
}
