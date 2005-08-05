/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.swa;

/**
 * @author <a href="mailto:thilina@opensource.lk">Thilina Gunarathne </a>
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.ParameterImpl;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.integration.UtilServer;
import org.apache.axis2.om.OMText;
import org.apache.axis2.om.impl.llom.OMTextImpl;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.axis2.receivers.RawXMLINOutMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wsdl.WSDLService;

public class EchoRawSwATest extends TestCase {
    private EndpointReference targetEPR = new EndpointReference(
            "http://127.0.0.1:" + (UtilServer.TESTING_PORT)
                    + "/axis/services/EchoSwAService/echoAttachment");

    private Log log = LogFactory.getLog(getClass());

    private QName serviceName = new QName("EchoSwAService");

    private QName operationName = new QName("echoAttachment");
    
    private ServiceContext serviceContext;

    private ServiceDescription service;

    private boolean finish = false;

    private OMTextImpl expectedTextData;

    public EchoRawSwATest() {
        super(EchoRawSwATest.class.getName());
    }

    public EchoRawSwATest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        UtilServer.start(Constants.TESTING_PATH + "MTOM-enabledRepository");
        service = new ServiceDescription(serviceName);
        service.setClassLoader(Thread.currentThread().getContextClassLoader());
        service
                .addParameter(new ParameterImpl(
                        AbstractMessageReceiver.SERVICE_CLASS, EchoSwA.class
                                .getName()));

        OperationDescription axisOp = new OperationDescription(operationName);
        axisOp.setMessageReciever(new RawXMLINOutMessageReceiver());
        axisOp.setStyle(WSDLService.STYLE_DOC);
        service.addOperation(axisOp);
        UtilServer.deployService(service);
        serviceContext = UtilServer.getConfigurationContext()
                .createServiceContext(service.getName());

    }

    protected void tearDown() throws Exception {
        UtilServer.unDeployService(serviceName);
        UtilServer.stop();
    }

    public void testEchoXMLSync() throws Exception {
        Socket socket = new Socket("127.0.0.1", 5555);
        OutputStream outStream = socket.getOutputStream();
        InputStream inStream = socket.getInputStream();
        InputStream requestMsgInStream = getResourceAsStream("org/apache/axis2/swa/swainput.txt");
        while (requestMsgInStream.available() > 0) {
            int data = requestMsgInStream.read();
            outStream.write(data);
        }
        outStream.flush();
        socket.shutdownOutput();
        byte[] i =  new byte[1];
        StringBuffer stringBuffer =  new StringBuffer();
        while ((i[0] = (byte)inStream.read()) != -1) {
            stringBuffer.append(new String(i));
        }
        socket.close();
        assertTrue(stringBuffer.toString().indexOf("Apache Axis2 - The NExt Generation Web Services Engine")>0);
        assertTrue(stringBuffer.toString().indexOf("multipart/related")>0);
    }

    private InputStream getResourceAsStream(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl.getResourceAsStream(path);
    }

    private void compareWithCreatedOMText(OMText actualTextData) {
        String originalTextValue = expectedTextData.getText();
        String returnedTextValue = actualTextData.getText();
        TestCase.assertEquals(returnedTextValue, originalTextValue);
    }
}