import oracle.oats.scripting.modules.basic.api.internal.*;
import oracle.oats.scripting.modules.basic.api.*;
import oracle.oats.scripting.modules.http.api.*;
import oracle.oats.scripting.modules.http.api.HTTPService.*;
import oracle.oats.scripting.modules.http.api.exceptions.MatchException;
import oracle.oats.scripting.modules.utilities.api.*;
import oracle.oats.scripting.modules.utilities.api.sql.*;
import oracle.oats.scripting.modules.utilities.api.xml.*;
import oracle.oats.scripting.modules.utilities.api.file.*;

public class script extends IteratingVUserScript {
	@ScriptService oracle.oats.scripting.modules.utilities.api.UtilitiesService utilities;
	@ScriptService oracle.oats.scripting.modules.http.api.HTTPService http;
	
	public void initialize() throws Exception {
		getVariables().set("url", "http://169.254.153.53:1234",
				Variables.Scope.GLOBAL);
		http
				.setUserAgent("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
		http.setAcceptLanguage("en-IN");
		http.addValidator(new ResourceValidator());
	}
	
	/**
	 * Add code to be executed each iteration for this virtual user.
	 */
	public void run() throws Exception {

		beginStep("[1] Home", 0);
		{
			http.get(2, "{{url,http://169.254.153.53/default}}", null, null, true, "UTF-8", "UTF-8");
			{
				http.assertText("Home", "<title>Default - Index</title>",
						Source.Html, TextPresence.PassIfPresent,
						MatchOption.Exact);
			}
			http.beginConcurrent("Home_Resources");
			http.get(9, "{{url,http://169.254.153.53/default}}/css/style.css", null, null, true, "UTF-8", "ASCII");
			http.get(13, "{{url,http://169.254.153.53/default}}/images/indx.png", null, null, true, "UTF-8", "GB18030");
			http.get(29, "{{url,http://169.254.153.53/default}}/images/two.jpg", null, null, true, "UTF-8", "windows-1252");
			http.get(17, "{{url,http://169.254.153.53/default}}/images/one.png", null, null, true, "UTF-8", "GB18030");
			http.get(25, "{{url,http://169.254.153.53/default}}/images/four.bmp", null, null, true, "UTF-8", "GB18030");
			http.get(21, "{{url,http://169.254.153.53/default}}/images/three.gif", null, null, true, "UTF-8", "windows-1252");
			http.endConcurrent("Home_Resources");
		}
		endStep();
		beginStep("[2] Page1", 0);
		{
			
			http
					.get(
							33, "{{url,http://169.254.153.53/default}}/page1.html", null, null, true, "UTF-8", "UTF-8");
			{
				http.assertText("Page1", "<title>Default - Page1</title>",
						Source.Html, TextPresence.PassIfPresent,
						MatchOption.Exact);
			}
			
			http.beginConcurrent("Page1_Resources");
			http.get(49, "{{url,http://169.254.153.53/default}}/css/style.css", null, null, true, "UTF-8", "ASCII");
			http.get(45, "{{url,http://169.254.153.53/default}}/images/pg1.png", null, null, true, "UTF-8", "GB18030");
			http.get(41, "{{url,http://169.254.153.53/default}}/images/seven.gif", null, null, true, "UTF-8", "windows-1252");
			http.get(57, "{{url,http://169.254.153.53/default}}/images/eight.bmp", null, null, true, "UTF-8", "GB18030");
			http.get(37, "{{url,http://169.254.153.53/default}}/images/six.jpg", null, null, true, "UTF-8", "windows-1252");
			http.get(53, "{{url,http://169.254.153.53/default}}/images/five.png", null, null, true, "UTF-8", "GB18030");
			http.endConcurrent("Page1_Resources");
		}
		endStep();
		beginStep("[3] Animation", 0);
		{

			http
					.get(
							61, "{{url,http://169.254.153.53/default}}/animation.html", null, null, true, "UTF-8", "UTF-8");
			{
				http.assertText("Animation",
						"<title>Default - Animation</title>", Source.Html,
						TextPresence.PassIfPresent, MatchOption.Exact);
			}
			
			http.beginConcurrent("Animation_Resources");
			http.get(85, "{{url,http://169.254.153.53/default}}/js/jquery_min.js", null, null, true, "UTF-8", "ASCII");
			http.get(81, "{{url,http://169.254.153.53/default}}/images/animthree.jpg", null, null, true, "UTF-8", "windows-1252");
			http.get(77, "{{url,http://169.254.153.53/default}}/images/animone.jpg", null, null, true, "UTF-8", "windows-1252");
			http.get(73, "{{url,http://169.254.153.53/default}}/images/animfour.jpg", null, null, true, "UTF-8", "windows-1252");
			http.get(69, "{{url,http://169.254.153.53/default}}/images/animtwo.jpg", null, null, true, "UTF-8", "windows-1252");
			http.endConcurrent("Animation_Resources");
		}
		endStep();
		
	}
	
	public void finish() throws Exception {
	}
}

class ResourceValidator implements IValidator {
	@ScriptService oracle.oats.scripting.modules.http.api.HTTPService http;
	@ScriptService oracle.oats.scripting.modules.utilities.api.UtilitiesService utilities;

	public ResourceValidator() {}
	
	public void validate(Response response) {
		// validate all requests except requests for html pages
		if(!response.getContentType().equalsIgnoreCase("text/html")){
			//eg: http://xyz.com/images/one.gif
			String url = response.getRequest().getUrl();
			String[] urlTokens = url.split("/");
			
			// {"one", "gif"}
			String[] resourceNameTokens = urlTokens[urlTokens.length-1].split("\\.");
			
			// eg: one
			String resourceName = resourceNameTokens[0];
			
			// ONE
			ResourceName resEnum = ResourceName.valueOf(resourceName.toUpperCase());
			
			String refLastModTime = resEnum.getLastModifiedTime();
			String respLastModTime = response.getHeaders().get("Last-Modified");
			if(!respLastModTime.equalsIgnoreCase(refLastModTime)){
				response.setException(new MatchException("Assertion failed for the resource " + resourceName));
			}
		}
	}
}

enum ResourceName{
	ONE("Fri, 23 Sep 2011 10:23:17 +0000"),
	TWO("Fri, 23 Sep 2011 10:25:36 +0000"),
	THREE("Fri, 23 Sep 2011 10:26:40 +0000"),
	FOUR("Fri, 23 Sep 2011 10:30:28 +0000"),
	FIVE("Fri, 23 Sep 2011 11:02:49 +0000"),
	SIX("Fri, 23 Sep 2011 11:04:19 +0000"),
	SEVEN("Fri, 23 Sep 2011 11:05:18 +0000"),
	EIGHT("Fri, 23 Sep 2011 11:06:46 +0000"),
	INDX("Fri, 23 Sep 2011 11:42:19 +0000"),
	PG1("Fri, 23 Sep 2011 11:43:02 +0000"),
	STYLE("Sat, 24 Sep 2011 03:41:37 +0000"),
	JQUERY_MIN("Thu, 19 Feb 2009 23:02:21 +0000"),
	ANIMONE("Fri, 8 May 2009 05:41:18 +0000"),
	ANIMTWO("Fri, 8 May 2009 09:30:40 +0000"),
	ANIMTHREE("Fri, 8 May 2009 05:40:38 +0000"),
	ANIMFOUR("Fri, 8 May 2009 09:34:40 +0000");
	
	ResourceName(String lastModifiedTime){
		this.lastModifiedTime = lastModifiedTime;
	}
	
	String getLastModifiedTime(){
		return lastModifiedTime;
	}
	
	private String lastModifiedTime;
	@ScriptService oracle.oats.scripting.modules.http.api.HTTPService http;
	@ScriptService oracle.oats.scripting.modules.utilities.api.UtilitiesService utilities;
}
