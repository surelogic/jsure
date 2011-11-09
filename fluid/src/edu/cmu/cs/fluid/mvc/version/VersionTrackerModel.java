package edu.cmu.cs.fluid.mvc.version;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.version.Version;

/**
 * A model that encapsulates a version variable. The model contains no nodes.
 * The model-level attribute {@link #VERSION}contains the {@link Version}
 * value encapsulated by the model. This model is roughly analogous to
 * {@link edu.cmu.cs.fluid.version.VersionTracker}.
 * 
 * <P>
 * An implementation must support the model-level attributes:
 * <ul>
 * <li>{@link #MODEL_NAME}
 * <li>{@link #MODEL_NODE}
 * <li>{@link #VERSION}
 * </ul>
 * 
 * <P>
 * An implementation must support the node-level attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * </ul>
 * 
 * @author Aaron Greenhouse
 * @author Zia Syed
 */
public interface VersionTrackerModel extends Model {
  /**
	 * Canonical reference to the Category representing versioned
	 * model&ndash;view&ndash;controller related output.
	 */
  public static final Logger LOG = SLLogger.getLogger("MV.version");

  /**
	 * Mutable {@link Version}-valued model-level attribute indicating the
	 * version represented by this model.
	 */
  public static final String VERSION = "VersionTrackerModel.VERSION";

  /**
	 * Convienence method for setting the value of the {@link #VERSION}
	 * attribute.
	 */
  public void setVersion(Version version);

  /**
	 * Convienence method for getting the value of the {@link #VERSION}
	 * attribute.
	 */
  public Version getVersion();

  //  /**
  //   * Runs the Runnable within the context of the model's version, possibly
	// updating the tracker
  //   */
  //  public void executeWithin(Runnable r);
}
