/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.apache.synapse.util.xpath;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.OMElementUtils;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.jaxen.JaxenException;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynapseDynamicPath {

    public static final String DEFAULT_CONTEXT = "DEFAULT";
    public static final String SYNAPSE_CONTEXT = "SYNAPSE";
    public static final String AXIS2_CONTEXT = "AXIS2";
    public static final String AXIS2_CLIENT_CONTEXT = "AXIS2-CLIENT";
    public static final String TRANSPORT_CONTEXT = "TRANSPORT";
    public static final String OPERATIONS_CONTEXT = "OPERATION";
    public static final String TRANSPORT_HEADERS = "TRANSPORT_HEADERS";
    public static final String FUNCTION_CONTEXT = "FUNC";
    public static final String EMPTY_STRING = "";
    private static final Log log = LogFactory.getLog(SynapseDynamicPath.class);
    private String xpathExpr;
    private boolean isJSON;
    private OMElement namespace;
    private String currentXPathValue;

    public SynapseDynamicPath(String xpathExpr, Boolean isJSON) {
        this.xpathExpr = xpathExpr;
        this.isJSON = isJSON;
    }

    public void setNamespace(OMElement namespace) {
        this.namespace = namespace;
    }

    /**
     * Redmine-975
     */
    public void includeDynamicProperties(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        HashMap functionProperties = new HashMap();
        Stack<TemplateContext> templeteContextStack = ((Stack) synCtx
                .getProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK));
        if (templeteContextStack != null && !templeteContextStack.isEmpty()) {
            TemplateContext templateContext = templeteContextStack.peek();
            functionProperties.putAll(templateContext.getMappedValues());
        }

        Matcher dynamicValues = getDynamicValuesFromExpression();
        String[] propertyScopeAndName;
        Object value;
        currentXPathValue = xpathExpr;

        while (dynamicValues.find()) {
            propertyScopeAndName = (dynamicValues.group(1)).split(":");

            if (propertyScopeAndName.length == 1) {
                propertyScopeAndName = new String[] { DEFAULT_CONTEXT, propertyScopeAndName[0] };
            }

            switch (propertyScopeAndName[0].toUpperCase()) {
            case DEFAULT_CONTEXT:
            case SYNAPSE_CONTEXT:
                value = synCtx.getProperty(propertyScopeAndName[1]);
                break;
            case TRANSPORT_CONTEXT:
                value = ((Map) axis2MsgCtx.getProperty(TRANSPORT_HEADERS)).get(propertyScopeAndName[1]);
                break;
            case AXIS2_CONTEXT:
                value = axis2MsgCtx.getProperty(propertyScopeAndName[1]);
                break;
            case AXIS2_CLIENT_CONTEXT:
                value = axis2MsgCtx.getOptions().getProperty(propertyScopeAndName[1]);
                break;
            case OPERATIONS_CONTEXT:
                value = axis2MsgCtx.getOperationContext().getProperty(propertyScopeAndName[1]);
                break;
            case FUNCTION_CONTEXT:
                value = functionProperties.get(propertyScopeAndName[1]);
                break;
            default:
                log.warn(propertyScopeAndName[0] + " scope is not found. Setting it to an empty value.");
                value = EMPTY_STRING;
            }
            currentXPathValue = currentXPathValue.replaceFirst("\\{" + dynamicValues.group(1) + "\\}", value.toString());
        }

    }

    public SynapsePath buildDynamicXPath() throws JaxenException {
        SynapsePath synapsePath;
        if (isJSON) {
            synapsePath = new SynapseJsonPath(currentXPathValue.substring(10, xpathExpr.length() - 1));
        } else {
            synapsePath = new SynapseXPath(currentXPathValue);
        }

        OMElementUtils.addNameSpaces(synapsePath, namespace, log);
        synapsePath.addNamespacesForFallbackProcessing(namespace);

        return synapsePath;
    }

    private Matcher getDynamicValuesFromExpression() {
        Pattern p = Pattern.compile("\\{(.*?)\\}");
        return p.matcher(xpathExpr);
    }

    public String getXpathExpr() {
        return this.xpathExpr;
    }

}