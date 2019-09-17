package net.kenevans.maplines.plugin;

/**
 * Constant definitions for plug-in preferences. Names of preferences start with
 * P_ and default values start with D_.
 * 
 * Map Lines is using preferences only to retain current values for the next
 * run.
 */
public interface IPreferenceConstants {
	/** The name of the image file */
	public static final String P_IMAGE_FILE_NAME = "imageFileName";
	public static final String P_CALIB_FILE_NAME = "calibFileName";
	public static final String P_INITIAL_IMAGE_PATH = "initialImagePath";
}
