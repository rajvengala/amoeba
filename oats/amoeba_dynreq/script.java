import oracle.oats.scripting.modules.basic.api.internal.*;
import oracle.oats.scripting.modules.basic.api.*;
import oracle.oats.scripting.modules.http.api.*;
import oracle.oats.scripting.modules.http.api.HTTPService.*;
import oracle.oats.scripting.modules.utilities.api.*;
import oracle.oats.scripting.modules.utilities.api.sql.*;
import oracle.oats.scripting.modules.utilities.api.xml.*;
import oracle.oats.scripting.modules.utilities.api.file.*;

public class script extends IteratingVUserScript {
	@ScriptService oracle.oats.scripting.modules.utilities.api.UtilitiesService utilities;
	@ScriptService oracle.oats.scripting.modules.http.api.HTTPService http;
	
	public void initialize() throws Exception {
	}
	
	/**
	 * Add code to be executed each iteration for this virtual user.
	 */
	public void run() throws Exception {
		
		if (false) {
			beginStep("Get Request", 0);
			{
				http.get(2, "http://169.254.153.53:1234/classes/TestDynRequests", http.querystring(http.param("one", "one&two"), http
										.param("two", "two&three")), null, true, "UTF-8", "UTF-8");
			}
			endStep();
		}
		if (false) {
			beginStep("Post Request", 0);
			{
				http.post(4, "http://169.254.153.53:1234/classes/TestDynRequests", http.querystring(http.param("one",
														"one&two"), http.param(
														"two", "two&three")), http.postdata(http.param("three",
														"three&four"), http.param(
														"four", "four&five")), null, true, "UTF-8", "UTF-8");
			}
			endStep();
		}
		beginStep("MultipartPost Request", 0);
		{
			http.multipartPost(5,
					"http://169.254.153.53:1234/classes/TestDynRequests", http
							.querystring(http.param("one", "one&two"), http
									.param("two", "two&three")),
					http.postdata(http.param("five", "five&six"), http.param(
							"six", "six&seven"), http.param("filename",
							"C:\\Users\\rvengala\\Desktop\\Inquest.docx",
							"C:\\Users\\rvengala\\Desktop\\Inquest.docx", "")),
					null, "ASCII", "ASCII");
		}
		endStep();
		
	}
	
	public void finish() throws Exception {
	}
}
