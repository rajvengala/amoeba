/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package in.uglyhunk.amoeba.server;

import java.io.IOException;


/**
 *
 * @author rvengala
 */
public class PartialRequest {
 
    /**
     * @return the bodyLength
     */
    public int getTotalBodyLength() {
        return totalBodyLength;
    }

    /**
     * @param bodyLength the bodyLength to set
     */
    public void setTotalBodyLength(int totalBodyLength) {
        this.totalBodyLength = totalBodyLength;
    }

    /**
     * @return the body
     */
    public byte[] getRequestBytes() {
        return requestBytes;
    }

    /**
     * @param body the body to set
     */
    public void setRequestBytes(byte[] partialRequest) throws IOException{
        if(this.requestBytes == null){
            this.requestBytes = partialRequest;
        } else {
            int newSize = partialRequest.length;

            // temp byte array of old contents
            byte[] temp = this.requestBytes.clone();

            // create a new byte array with increase size
            this.requestBytes = new byte[newSize + temp.length];

            // combine old and new byte arrays
            System.arraycopy(temp, 0, this.requestBytes, 0, temp.length);
            System.arraycopy(partialRequest, 0, this.requestBytes, temp.length, partialRequest.length);
        }
        
    }
    
    /**
     * @return the partialBodyLength
     */
    public int getPartialBodyLength() {
        return partialBodyLength;
    }

    /**
     * @param partialBodyLength the partialBodyLength to set
     */
    public void setPartialBodyLength(int partialBodyLength) {
        this.partialBodyLength += partialBodyLength;
    }
    
    
    /**
     * @return the isBody
     */
    public boolean isBody() {
        return isBody;
    }

    /**
     * @param isBody the isBody to set
     */
    public void setIsBody(boolean isBody) {
        this.isBody = isBody;
    }
    
    private int totalBodyLength;
    private byte[] requestBytes;
    private int partialBodyLength;
    private boolean isBody;
}
