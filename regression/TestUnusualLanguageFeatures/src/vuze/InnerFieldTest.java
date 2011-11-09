package vuze;

import java.net.URL;

public class InnerFieldTest {
	private static volatile provider	provider;
	
	public static void
	setProvider(
		provider		_p )
	{
		provider	= _p;
	}
	
	public static provider
	getProvider()
	{
		return( provider );
	}
	
	public interface
	provider
	{
		public void
		subscribeToRSS(
			String		name,
			URL 		url,
			int			interval,
			boolean		is_public,
			String		creator_ref )
		
			throws Exception;
		
		public boolean
		canShowCDP(
			DownloadManager		dm );
		
		public void
		showCDP(
			DownloadManager		dm,
			String				ref );
		
		public String
		getCDPURL(
			DownloadManager		dm );
	}
}

interface DownloadManager {	
}