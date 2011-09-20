/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.dyn;

import in.uglyhunk.amoeba.server.RequestBean;
import in.uglyhunk.amoeba.server.ResponseBean;

/**
 * Implement this interface to process dynamic GET and POST requests
 *
 * @author rvengala
 */
public interface DynamicRequest {
    public void process(RequestBean request, ResponseBean response);
}
