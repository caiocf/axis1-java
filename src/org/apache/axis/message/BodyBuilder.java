package org.apache.axis.message;

/**
 * 
 * @author Glen Daniels (gdaniels@allaire.com)
 */

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.axis.Constants;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.ServiceDescription;
import org.apache.axis.utils.Debug;

public class BodyBuilder extends SOAPHandler
{
    private final static boolean DEBUG_LOG = false;
    
    private SOAPBodyElement element;
    boolean gotRPCElement = false;
    boolean isRPCElement = false;
    
    public SOAPHandler onStartChild(String namespace,
                                     String localName,
                                     Attributes attributes,
                                     DeserializationContext context)
        throws SAXException
    {
        if (DEBUG_LOG) {
            System.err.println("In BodyBuilder.onStartChild()");
        }
        SOAPHandler handler = null;
        
        /** We're about to create a body element.  So we really need
         * to know at this point if this is an RPC service or not.  It's
         * possible that no one has set the service up until this point,
         * so if that's the case we should attempt to set it based on the
         * namespace of the first root body element.  Setting the
         * service may (should?) result in setting the service
         * description, which can then tell us what to create.
         */
        boolean isRoot = true;
        String root = attributes.getValue(Constants.URI_SOAP_ENC,
                                        Constants.ATTR_ROOT);
        if ((root != null) && root.equals("0")) isRoot = false;

        if (isRoot &&
            context.getMessageContext().getServiceHandler() == null) {
            Debug.Print(2, "Dispatching to body namespace '",
                        namespace, "'");
            context.getMessageContext().setTargetService(namespace);
        }
        
        /** Now we make a plain SOAPBodyElement IF we either:
         * a) have an non-root element, or
         * b) have a non-RPC service
         */
        ServiceDescription serviceDesc = context.getMessageContext().
                                                      getServiceDescription();

        if (localName.equals(Constants.ELEM_FAULT) &&
            namespace.equals(Constants.URI_SOAP_ENV)) {
            element = new SOAPFaultElement(namespace, localName,
                                           attributes, context);
            handler = new SOAPFaultBuilder((SOAPFaultElement)element);
        } else if (!gotRPCElement &&
            isRoot && 
            ((serviceDesc == null) || (serviceDesc.isRPC()))) {
            gotRPCElement = true;
            element = new RPCElement(namespace, localName,
                                     attributes, context);
            handler = new RPCHandler((RPCElement)element);
        } else {
            element = new SOAPBodyElement(namespace, localName,
                                      attributes, context);
            if (element.getFixupDeserializer() != null)
                handler = element.getFixupDeserializer();
        }
        
        if (DEBUG_LOG) {
            System.err.println("Out BodyBuilder.onStartChild()");
        }
        return handler;
    }
    
    public void onEndChild(String namespace, String localName,
                           DeserializationContext context)
    {
        if (DEBUG_LOG) {
            System.err.println("In BodyBuilder.onEndChild()");
        }
        
        if (element != null) {
            element.setEndIndex(context.getCurrentRecordPos());
            context.envelope.addBodyElement(element);
            element = null;
        }

        if (DEBUG_LOG) {
            System.err.println("Out BodyBuilder.onEndChild()");
        }
    }
}
