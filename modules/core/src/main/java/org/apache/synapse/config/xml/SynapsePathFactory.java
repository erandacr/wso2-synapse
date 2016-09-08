/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.xpath.SynapseDynamicPath;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * 
 */
public class SynapsePathFactory {

    private static final Log log = LogFactory.getLog(SynapsePathFactory.class);

    public static SynapsePath getSynapsePath(OMElement elem, QName attribName)
        throws JaxenException {

        SynapsePath path = null;
        OMAttribute pathAttrib = elem.getAttribute(attribName);
        String expression;

        if (pathAttrib != null && pathAttrib.getAttributeValue() != null) {
            expression = pathAttrib.getAttributeValue();

            if (pathAttrib.getAttributeValue().contains("/{") || pathAttrib.getAttributeValue().contains(":{")) {
                expression = expression.replace("{","");
                expression = expression.replace("}","");
            }

            if (expression.startsWith("json-eval(")) {
                path = new SynapseJsonPath(
                        expression.substring(10, pathAttrib.getAttributeValue().length() - 1));
            } else {
                path = new SynapseXPath(expression);
            }

            OMElementUtils.addNameSpaces(path, elem, log);
            path.addNamespacesForFallbackProcessing(elem);


        } else {
            handleException("Couldn't find the XPath attribute with the QName : "
                + attribName.toString() + " in the element : " + elem.toString());
        }       

        return path;
    }

    public static SynapsePath getSynapsePath(OMElement elem, String expression)
        throws JaxenException {

        if (expression == null) {
            handleException("XPath expression cannot be null");
        }


        SynapseXPath xpath = new SynapseXPath(expression);
        OMElementUtils.addNameSpaces(xpath, elem, log);

        return xpath;
    }

    private static void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }

    public static SynapseDynamicPath getSynapseDynamicPath(OMElement elem, QName attribName) {
        OMAttribute pathAttrib = elem.getAttribute(attribName);
        SynapseDynamicPath synapseDynamicPath = null;

        if (pathAttrib != null && pathAttrib.getAttributeValue() != null) {

            if (pathAttrib.getAttributeValue().startsWith("json-eval(")) {
                synapseDynamicPath = new SynapseDynamicPath(
                        pathAttrib.getAttributeValue().substring(10, pathAttrib.getAttributeValue().length() - 1), true);
            } else {
                synapseDynamicPath = new SynapseDynamicPath(pathAttrib.getAttributeValue(), false);
            }
            synapseDynamicPath.setNamespace(elem);

        }
        return synapseDynamicPath;
    }

    public static boolean isDynamicExpression(OMElement elem, QName attribName) {
        OMAttribute pathAttrib = elem.getAttribute(attribName);

        if (pathAttrib != null && pathAttrib.getAttributeValue() != null) {
            return (pathAttrib.getAttributeValue().contains("/{") || pathAttrib.getAttributeValue().contains(":{"));
        }

        return false;
    }

    }
