//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.08.24 at 10:15:40 AM CEST 
//


package fr.inria.edelweiss.rif.xml.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for content-TERM.type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="content-TERM.type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2007/rif#}Expr"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "content-TERM.type", propOrder = {
    "expr"
})
public class ContentTERMType {

    @XmlElement(name = "Expr", required = true)
    protected Expr expr;

    /**
     * Gets the value of the expr property.
     * 
     * @return
     *     possible object is
     *     {@link Expr }
     *     
     */
    public Expr getExpr() {
        return expr;
    }

    /**
     * Sets the value of the expr property.
     * 
     * @param value
     *     allowed object is
     *     {@link Expr }
     *     
     */
    public void setExpr(Expr value) {
        this.expr = value;
    }
    
    public fr.inria.edelweiss.rif.ast.Function XML2AST() {
    	return this.expr.XML2AST() ;
    }

}
