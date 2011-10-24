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
		
		beginStep("Get Request", 0);
		{
			http.get(2, "http://169.254.153.53:1234/classes/TestDynRequests", http
																							.querystring(
																									http
																											.param(
																													"one",
																													"one&two"),
																									http
																											.param(
																													"two",
																													"two&three")), null, true, "UTF-8", "UTF-8");
			{
				http
						.assertText(
								"Get",
								"Query String</br>One - one&two</br>Two - two&three</br>Response Body</br>Three - null</br>Four - null</br>Multipart Request Body</br>Five - null</br>Six - null</br>Filename - null</br>",
								Source.Html, TextPresence.PassIfPresent,
								MatchOption.Exact);
			}
		}
		endStep();
		beginStep("Post Request", 0);
		{
			http.post(4, "http://169.254.153.53:1234/classes/TestDynRequests", http.querystring(http.param("one",
													"one&two"), http.param(
													"two", "two&three")), http.postdata(http.param("three",
													"three&four"), http.param(
													"four", "four&five")), null, true, "UTF-8", "UTF-8");
			{
				http
						.assertText(
								"Post",
								"Query String</br>One - one&two</br>Two - two&three</br>Response Body</br>Three - three&four</br>Four - four&five</br>Multipart Request Body</br>Five - null</br>Six - null</br>Filename - null</br>",
								Source.Html, TextPresence.PassIfPresent,
								MatchOption.Exact);
			}
		}
		endStep();
		beginStep("MultipartPost Request", 0);
		{
			http.multipartPost(5, "http://169.254.153.53:1234/classes/TestDynRequests", http
																																																																																																																																																																			.querystring(
																																																																																																																																																																					http
																																																																																																																																																																							.param(
																																																																																																																																																																									"one",
																																																																																																																																																																									"one&two"),
																																																																																																																																																																					http
																																																																																																																																																																							.param(
																																																																																																																																																																									"two",
																																																																																																																																																																									"two&three")), http
																																																																																																																																																																			.postdata(
																																																																																																																																																																					http
																																																																																																																																																																							.param(
																																																																																																																																																																									"five",
																																																																																																																																																																									"five&six"),
																																																																																																																																																																					http
																																																																																																																																																																							.param(
																																																																																																																																																																									"six",
																																																																																																																																																																									"six&seven"),
																																																																																																																																																																					http
																																																																																																																																																																							.param(
																																																																																																																																																																									"fname",
																																																																																																																																																																									"C:\\Users\\rvengala\\Desktop\\ATS Pack and Internal Adoption.pptx",
																																																																																																																																																																									"ppt",
																																																																																																																																																																									"application/octet-stream")), null, "ASCII", "ASCII");
			{
				http.assertText("multipart",
						"Filename - D:\\myprojects\\amoeba\\dist\\amoeba\\tmp",
						Source.DisplayContent, TextPresence.PassIfPresent,
						MatchOption.Exact);
			}
		}
		endStep();
		
	}
	
	public void finish() throws Exception {
	}
}
