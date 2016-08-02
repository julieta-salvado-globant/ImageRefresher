package eps;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author l.marino on 7/2/15.
 */
public class EPSClient {

    private String url = "https://api.sandbox.ebay.com/ws/api.dll";

    private String token = "";

    private String uploadDays = "1015";

    private String extensionDays = "1015";

    private String apiVersion = "517";

    private String siteId = "0";

    private String devId = "";

    private String appId = "";

    private String certId = "";

    private String verb = "UploadSiteHostedPictures";

    private String verbExtend = "ExtendSiteHostedPictures";

    private String ackTypeAccepted = "Success,Warning";

    private int timeOut = 30;

    /**
     * Post an image to EPS from a Picture File.
     *
     * @param name
     * @param file
     * @return url
     */
    public String publish(String name, MultipartFile file) {
        HttpPost postMethod = buildPost(name, "", "", file, true);
        return postToEPS(postMethod);
    }

    /**
     * Post an image to EPS from a url.
     *
     * @param name
     * @param externalUrl
     * @return
     */
    public String publishFromUrl(String name, String externalUrl) {
        HttpPost postMethod = buildPost(name, "", externalUrl, null, true);
        return postToEPS(postMethod);
    }

    /**
     * Refresh picture's expire time into EPS.
     *
     * @param picUrl the EPS image's url to update
     * @return picUrl
     */
    public String update(String picUrl) {
        HttpPost postMethod = buildPost("", picUrl, "", null, false);
        return postToEPS(postMethod);
    }

    private String postToEPS(HttpPost request){

        String pictureUrl = "";
        CloseableHttpResponse response = null;

        try {

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeOut * 1000)
                    .setConnectionRequestTimeout(timeOut * 1000)
                    .setSocketTimeout(timeOut * 1000).build();

            CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            //Post Image
            response = httpClient.execute(request);
            System.out.println("Post Image response: " + response.toString());

            HttpEntity entity = response.getEntity();
            
            String message = EntityUtils.toString(entity);
            System.out.println("Post Image response: " + message);

            //Retrieve image url
            pictureUrl = handleResponse(message);

            //Consume & close stream
            EntityUtils.consume(entity);

        } catch (Exception e) {
            System.out.println(
                    "An exception occurred when trying to connect to the EPS"
                    );
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                System.out.println("An exception occurred when trying to close the connection to the EPS");
            }
        }

        return pictureUrl;

    }

    private HttpPost buildPost(String picName, String picUrl, String externalPicUrl, MultipartFile file, boolean isPost) {

        HttpPost filePost = new HttpPost(url);

        String xml;

        if (isPost) {
            xml = buildXML(picName, picUrl, externalPicUrl, verb, uploadDays, apiVersion, token);
            setHeaders(filePost, apiVersion, devId,
                    appId, certId, verb, siteId);
        } else {
            xml = buildXML(picName, picUrl, externalPicUrl, verbExtend, extensionDays, apiVersion, token);
            setHeaders(filePost, apiVersion, devId,
                    appId, certId, verbExtend, siteId);
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("XML Payload", xml, ContentType.APPLICATION_XML);

        if (file != null) {
            try {
                builder.addBinaryBody("file", file.getBytes(), ContentType.APPLICATION_OCTET_STREAM, "file.ext");
            } catch (IOException e) {
                System.out.println(
                        "An exception occurred trying to open the picture before posting to EPS"
                        );
            }
        }

        HttpEntity entity = builder.build();

        filePost.setEntity(entity);

        return filePost;
    }

    private void setHeaders(HttpPost filePost, String apiVersion, String devId, String appId,
                            String certId, String verb, String siteId) {
        filePost.setHeader("SOAPAction", "");
        filePost.setHeader("X-EBAY-API-COMPATIBILITY-LEVEL", apiVersion);
        filePost.setHeader("X-EBAY-API-DEV-NAME", devId);
        filePost.setHeader("X-EBAY-API-APP-NAME", appId);
        filePost.setHeader("X-EBAY-API-CERT-NAME", certId);
        filePost.setHeader("X-EBAY-API-CALL-NAME", verb);
        filePost.setHeader("X-EBAY-API-SITEID", siteId);
    }

    private String buildXML(String picName, String picURL, String externalUrl, String verb, String days, String apiVersion, String token) {

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("\n");
        xml.append("<" + verb + "Request xmlns=\"urn:ebay:apis:eBLBaseComponents\">");
        xml.append("\n");
        xml.append("<Version>" + apiVersion + "</Version>");
        xml.append("\n");

        if (StringUtils.isNotEmpty(picName)) {
            xml.append("<PictureName>" + picName + "</PictureName>");
            if (!"".equals(externalUrl)) {
                xml.append("\n");
                xml.append("<ExternalPictureURL>" + externalUrl + "</ExternalPictureURL>");
            }
        } else {
            xml.append("<PictureURL>" + picURL + "</PictureURL>");
        }

        xml.append("\n");
        xml.append("<ExtensionInDays>" + days + "</ExtensionInDays>");
        xml.append("\n");
        xml.append("<RequesterCredentials><ebl:eBayAuthToken xmlns:ebl=\"urn:ebay:apis:eBLBaseComponents\"><![CDATA[");
        xml.append(token);
        xml.append("]]></ebl:eBayAuthToken></RequesterCredentials>");
        xml.append("\n");
        xml.append("</" + verb + "Request>");
        xml.append("\n");

        return xml.toString();
    }

    private String handleResponse(String outputResponse) throws Exception {

        String picUrl = "";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(new InputSource(new StringReader(outputResponse)));
        document.getDocumentElement().normalize();
        NodeList uploadSiteHostedPicturesResponse = document.getElementsByTagName("UploadSiteHostedPicturesResponse");
        NodeList extendSiteHostedPicturesResponse = document.getElementsByTagName("ExtendSiteHostedPicturesResponse");
        Element ackElement = (Element) document.getElementsByTagName("Ack").item(0);

        if (ackElement.getTextContent() != null
                && !ackTypeAccepted.contains(ackElement.getTextContent())) {
            NodeList errors = document.getElementsByTagName("Errors");

            if (null != errors) {
                Element errorElement = (Element) errors.item(0);
                String errorMessage = errorElement
                        .getElementsByTagName("LongMessage").item(0)
                        .getTextContent();

                System.out.println("EPS Error Payload " + outputResponse);
                throw new RuntimeException(errorMessage);
            }
        }

        if (uploadSiteHostedPicturesResponse.getLength() > 0) {
            picUrl = document.getElementsByTagName("FullURL").item(0)
                    .getTextContent();

        } else if (extendSiteHostedPicturesResponse.getLength() > 0) {
            picUrl = document.getElementsByTagName("PictureURL").item(0)
                    .getTextContent();
        }

        return picUrl;
    }
}