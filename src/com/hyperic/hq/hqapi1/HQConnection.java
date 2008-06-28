package com.hyperic.hq.hqapi1;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.net.SocketException;

public abstract class HQConnection {

    private static Log _log = LogFactory.getLog(HQConnection.class);

    private String  _host;
    private int     _port;
    private boolean _isSecure;
    private String  _user;
    private String  _password;

    HQConnection(String host, int port, boolean isSecure, String user,
                 String password) {
        _host     = host;
        _port     = port;
        _isSecure = isSecure;
        _user     = user;
        _password = password;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();

        client.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials(_user,
                                                                   _password);
        client.getState().setCredentials(AuthScope.ANY, defaultcreds);
        return client;
    }

    private Object deserialize(Class res, InputStream is)
        throws JAXBException
    {
        String pkg = res.getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(pkg);
        Unmarshaller u = jc.createUnmarshaller();
        u.setEventHandler(new DefaultValidationEventHandler());
        return res.cast(u.unmarshal(is));
    }

    private void serialize(Object o, OutputStream os)
        throws JAXBException
    {
        String pkg = o.getClass().getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(pkg);
        Marshaller m = jc.createMarshaller();
        m.setEventHandler(new DefaultValidationEventHandler());
        m.marshal(o, os);
    }

    private String urlEncode(String s)
        throws IOException
    {
        return URLEncoder.encode(s, "UTF-8");
    }

    Object doGet(String path, Map params, Class resultClass)
        throws IOException, JAXBException
    {
        GetMethod method = new GetMethod();
        method.setDoAuthentication(true);

        StringBuffer uri = new StringBuffer(path);
        if (uri.charAt(uri.length() - 1) != '?') {
            uri.append("?");
        }

        int idx = 0;
        for (Iterator i = params.keySet().iterator(); i.hasNext(); idx++) {
            String key = (String)i.next();
            String value = (String)params.get(key);
            if (value != null) {
                if (idx > 0) {
                    uri.append("&");
                }
                uri.append(key).append("=").append(urlEncode(value));
            }
        }

        return runMethod(method, uri.toString(), resultClass);
    }

    Object doPost(String path, Object o, Class resultClass)
        throws IOException, JAXBException
    {
        PostMethod method = new PostMethod();
        method.setDoAuthentication(true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serialize(o, bos);

        Part[] parts = {
            new StringPart("postdata", bos.toString())
        };

        method.setRequestEntity(new MultipartRequestEntity(parts,
                                                           method.getParams()));

        return runMethod(method, path, resultClass);
    }

    private Object runMethod(HttpMethodBase method, String uri,
                             Class resultClass)
        throws IOException, JAXBException
    {
        String protocol = _isSecure ? "https" : "http";

        URL url = new URL(protocol, _host, _port, uri);
        _log.info("HTTP Request: " + url.toString());
        method.setURI(new URI(url.toString(), true));

        int code;
        try {
            code = getHttpClient().executeMethod(method);
        } catch (SocketException e) {
            throw new HttpException("Error issuing request", e);
        }

        if (code == 200) {
            // We only deal with HTTP_OK responses
            if (resultClass != null) {
                if (_log.isInfoEnabled()) {
                    _log.info("HTTP Response: " +
                              method.getResponseBodyAsString());
                }
                
                InputStream is = method.getResponseBodyAsStream();
                return deserialize(resultClass, is);
            }
        } else {
            throw new HttpException("Invalid HTTP return code " + code);
        }

        return null;   
    }
}