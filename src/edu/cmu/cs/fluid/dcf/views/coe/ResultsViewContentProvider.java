package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.xml.results.coe.CoE_Constants;

import edu.cmu.cs.fluid.sea.*;

public class ResultsViewContentProvider 
extends GenericResultsViewContentProvider<Drop,Content> {
	public ResultsViewContentProvider() {
		super(Sea.getDefault());
	}
	
	/**
	 * Adds referenced supporting information about a drop to the mutable set of
	 * viewer content items passed into this method.
	 * 
	 * @param mutableContentSet
	 *            set of all {@link Content} items
	 * @param about
	 *            the {@link Drop}to add supporting information about
	 */
	private void addSupportingInformation(Content mutableContentSet,
			IRReferenceDrop about) {
		Collection<ISupportingInformation> supportingInformation = about
				.getSupportingInformation();
		int size = supportingInformation.size();
		if (size == 0) {
			// no supporting information, thus bail out
			return;
		} else if (size == 1) {
			ISupportingInformation si = supportingInformation.iterator().next();
			Content informationItem = new Content("supporting information: "
					+ si.getMessage(), si.getLocation());
			informationItem.setBaseImageName(CommonImages.IMG_INFO);
			mutableContentSet.addChild(informationItem);
			return;
		}
		// More than one thing
		Content siFolder = new Content("supporting information:");
		siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

		for (Iterator<ISupportingInformation> i = supportingInformation
				.iterator(); i.hasNext();) {
			ISupportingInformation si = i.next();
			Content informationItem = new Content(si.getMessage(), si
					.getLocation());
			informationItem.setBaseImageName(CommonImages.IMG_INFO);
			siFolder.addChild(informationItem);
		}
		// Improves the presentation in the view
		switch (siFolder.numChildren()) {
		case 0:
			return; // Don't add anything
		case 1:
			mutableContentSet.addChild((Content) siFolder.getChildren()[0]);
			mutableContentSet.addChild(siFolder);
			break;
		default:
			mutableContentSet.addChild(siFolder);
			break;
		}
	}

	/**
	 * Adds referenced proposed promises about a drop to the mutable set of
	 * viewer content items passed into this method.
	 * 
	 * @param mutableContentSet
	 *            set of all {@link Content} items
	 * @param about
	 *            the {@link Drop}to add proposed promises about
	 */
	private void addProposedPromises(Content mutableContentSet,
			IRReferenceDrop about) {
		Collection<ProposedPromiseDrop> proposals = about.getProposals();
		int size = proposals.size();
		if (size == 0) {
			// no proposed promises, thus bail out
			return;
		} else if (size == 1) {
			ProposedPromiseDrop pp = proposals.iterator().next();
			final Content proposalItem = new Content("proposed promise: "
					+ pp.getJavaAnnotation(), pp.getNode(), pp);
			proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
			mutableContentSet.addChild(proposalItem);
			return;
		}
		// More than one thing
    Content siFolder = new Content(I18N
        .msg("jsure.eclipse.proposed.promise.content.folder"));
		siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

		for (Iterator<ProposedPromiseDrop> i = proposals.iterator(); i
				.hasNext();) {
			ProposedPromiseDrop pp = i.next();
			final Content proposalItem = new Content(pp.getJavaAnnotation(), pp
					.getNode(), pp);
			proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
			siFolder.addChild(proposalItem);
		}
		// Improves the presentation in the view
		switch (siFolder.numChildren()) {
		case 0:
			return; // Don't add anything
		case 1:
			mutableContentSet.addChild((Content) siFolder.getChildren()[0]);
			mutableContentSet.addChild(siFolder);
			break;
		default:
			mutableContentSet.addChild(siFolder);
			break;
		}
	}

	/**
	 * Adds "and" precondition logic information about a drop to the mutable set
	 * of viewer content items passed into this method.
	 * 
	 * @param mutableContentSet
	 *            A parent {@link Content} object to add children to
	 * @param result
	 *            the result to add "and" precondition logic about
	 */
	@SuppressWarnings("unchecked")
	private void add_and_TrustedPromises(Content mutableContentSet,
			ResultDrop result) {
		// Create a folder to contain the preconditions
		Set<PromiseDrop> trustedPromiseDrops = result.getTrusts();
		int count = trustedPromiseDrops.size();
		// bail out if no preconditions exist
		if (count < 1)
			return;
		Content preconditionFolder = new Content(count
				+ (count > 1 ? " prerequisite assertions:"
						: " prerequisite assertion:"));
		int flags = 0; // assume no adornments
		flags |= (result.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
		boolean elementsProvedConsistent = true; // assume true

		// add trusted promises to the folder
		for (ProofDrop trustedDrop : trustedPromiseDrops) {
			// ProofDrop trustedDrop = (ProofDrop) j.next();
			preconditionFolder.addChild(encloseDrop(trustedDrop));
			elementsProvedConsistent &= trustedDrop.provedConsistent();
		}

		// finish up the folder
		flags |= (elementsProvedConsistent ? CoE_Constants.CONSISTENT
				: CoE_Constants.INCONSISTENT);
		preconditionFolder.setImageFlags(flags);
		preconditionFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
		mutableContentSet.addChild(preconditionFolder);
	}

	/**
	 * Adds "or" precondition logic information about a drop to the mutable set
	 * of viewer content items passed into this method.
	 * 
	 * @param mutableContentSet
	 *            A parent {@link Content} object to add children to
	 * @param result
	 *            the result to add "or" precondition logic about
	 */
	private void add_or_TrustedPromises(Content mutableContentSet,
			ResultDrop result) {
		if (!result.hasOrLogic()) {
			// no "or" logic on this result, thus bail out
			return;
		}

		// Create a folder to contain the choices
		final Set<String> or_TrustLabels = result.get_or_TrustLabelSet();
		final int or_TrustLabelsSize = or_TrustLabels.size();
		Content orContentFolder = new Content(
				or_TrustLabelsSize
						+ (or_TrustLabelsSize > 1 ? " possible prerequisite assertion choices:"
								: " possible prerequisite assertion choice:"));
		int flags = 0; // assume no adornments
		flags |= (result.get_or_proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
		flags |= (result.get_or_provedConsistent() ? CoE_Constants.CONSISTENT
				: CoE_Constants.INCONSISTENT);
		orContentFolder.setImageFlags(flags);
		orContentFolder.setBaseImageName(CommonImages.IMG_CHOICE);
		mutableContentSet.addChild(orContentFolder);

		// create a folder for each choice
		for (String key : or_TrustLabels) {
			// String key = (String) i.next();
			Content choiceFolder = new Content(key + ":");
			orContentFolder.addChild(choiceFolder);

			// set proof bits properly
			boolean choiceConsistent = true;
			boolean choiceUsesRedDot = false;
			Set<? extends ProofDrop> choiceSet = result.get_or_Trusts(key);

			// fill in the folder with choices
			for (ProofDrop trustedDrop : choiceSet) {
				// ProofDrop trustedDrop = (ProofDrop) j.next();
				choiceFolder.addChild(encloseDrop(trustedDrop));
				choiceConsistent &= trustedDrop.provedConsistent();
				if (trustedDrop.proofUsesRedDot())
					choiceUsesRedDot = true;
			}
			flags = (choiceUsesRedDot ? CoE_Constants.REDDOT : 0);
			flags |= (choiceConsistent ? CoE_Constants.CONSISTENT
					: CoE_Constants.INCONSISTENT);
			choiceFolder.setImageFlags(flags);
			choiceFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
		}
	}

	/**
	 * Create a {@link Content}item for a drop-sea drop. This is only done once,
	 * hence, the same Content item is returned if the same drop is passed to
	 * this method.
	 * 
	 * @param drop
	 *            the drop to enclose
	 * @return the content item the viewer can use
	 */
	@SuppressWarnings("unchecked")
	protected Content encloseDrop(Drop drop) {
		if (drop == null) {
			LOG
					.log(Level.SEVERE,
							"ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
			throw new IllegalArgumentException(
					"ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
		}
		Content result = getFromContentCache(drop);
		if (result != null) {
			// in cache
			return result;
		} else if (existsInCache(drop)) {
			return null;
		} else {
			// create & add to cache -- MUST BE IMMEDIATE TO AVOID INFINITE
			// RECURSION
			result = new Content(drop.getMessage(), drop);
			putInContentCache(drop, result); // to avoid infinite recursion

			if (drop instanceof PromiseDrop) {

				/*
				 * PROMISE DROP
				 */

				PromiseDrop promiseDrop = (PromiseDrop) drop;

				// image
				int flags = 0; // assume no adornments
				if (promiseDrop.isIntendedToBeCheckedByAnalysis()) {
					flags |= (promiseDrop.proofUsesRedDot() ? CoE_Constants.REDDOT
							: 0);
					flags |= (promiseDrop.provedConsistent() ? CoE_Constants.CONSISTENT
							: CoE_Constants.INCONSISTENT);
					flags |= (promiseDrop.isCheckedByAnalysis() ? 0
							: CoE_Constants.TRUSTED);
				}
				flags |= (promiseDrop.isAssumed() ? CoE_Constants.ASSUME : 0);
				flags |= (promiseDrop.isVirtual() ? CoE_Constants.VIRTUAL : 0);
				result.setImageFlags(flags);
				result.setBaseImageName(CommonImages.IMG_ANNOTATION);

				// children
				addSupportingInformation(result, promiseDrop);
				addProposedPromises(result, promiseDrop);

				Set<Drop> matching = new HashSet<Drop>();
				promiseDrop.addMatchingDependentsTo(matching,
						DropPredicateFactory.matchType(PromiseDrop.class));
				promiseDrop.addMatchingDependentsTo(matching,
						DropPredicateFactory.matchType(InfoDrop.class));
				addDrops(result, matching);
				addDrops(result, promiseDrop.getCheckedBy());

			} else if (drop instanceof ResultDrop) {

				/*
				 * RESULT DROP
				 */

				ResultDrop resultDrop = (ResultDrop) drop;

				// image
				int flags = 0; // assume no adornments
				if (resultDrop.getTrustsComplete().size() > 0) {
					// only show reddot and proof status if this results has
					// preconditions
					flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT
							: 0);
					flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT
							: CoE_Constants.INCONSISTENT);
				}
				result.setImageFlags(flags);
				result
						.setBaseImageName(resultDrop.isConsistent() ? CommonImages.IMG_PLUS
								: resultDrop.isVouched() ? CommonImages.IMG_PLUS_VOUCH
										: CommonImages.IMG_RED_X);

				// children
				addSupportingInformation(result, resultDrop);
				addProposedPromises(result, resultDrop);
				add_or_TrustedPromises(result, resultDrop);
				add_and_TrustedPromises(result, resultDrop);

			} else if (drop instanceof InfoDrop) {

				/*
				 * INFO DROP
				 */

				InfoDrop infoDrop = (InfoDrop) drop;

				// image
				result
						.setBaseImageName(drop instanceof WarningDrop ? CommonImages.IMG_WARNING
								: CommonImages.IMG_INFO);

				// children
				addSupportingInformation(result, infoDrop);
        addProposedPromises(result, infoDrop);

				result.f_isInfo = true;
				result.f_isInfoWarning = drop instanceof WarningDrop;

			} else if (drop instanceof PromiseWarningDrop) {

				/*
				 * PROMISE WARNING DROP
				 */

				PromiseWarningDrop promiseWarningDrop = (PromiseWarningDrop) drop;

				// image
				result.setBaseImageName(CommonImages.IMG_WARNING);

				// children
				addSupportingInformation(result, promiseWarningDrop);
				result.f_isPromiseWarning = true;
			} else {
				LOG.log(Level.SEVERE,
						"ResultsViewContentProvider.encloseDrop(Drop) passed an unknown drop type "
								+ drop.getClass());
			}
			return result;
		}
	}

	@Override
	protected boolean dropsExist(Class<? extends Drop> type) {
		return !Sea.getDefault().getDropsOfType(type).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <R extends IDropInfo>
	Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType) {
		return (Collection<R>) Sea.getDefault().getDropsOfType(type);
	}
	
	@Override
	protected Content makeContent(String msg) {
		return new Content(msg);
	}

	@Override
	protected Content makeContent(String msg, Collection<Content> contentRoot) {
		return new Content(msg, contentRoot);
	}
}