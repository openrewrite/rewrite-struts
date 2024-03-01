package org.openrewrite.java.struts;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.test.SourceSpecs.dir;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings("TrailingWhitespacesInTextBlock")
public class HelloWorldStrutsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpath("struts-core"))
          .recipe(Recipe.noop());
    }

    @Test
    void helloWorld() {
        rewriteRun(
          //language=java
          java(
            """
              package org.apache.struts.helloworld.model;
                            
              public class MessageStore {
                  private String message;
                            
                  public MessageStore() {
                      message = "Hello Struts User";
                  }
                            
                  public String getMessage() {
                      return message;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              package org.apache.struts.helloworld.action;
                            
              import org.apache.struts.helloworld.model.MessageStore;
              import com.opensymphony.xwork2.ActionSupport;
                            
              public class HelloWorldAction extends ActionSupport {
                  private MessageStore messageStore;
                  
                  public String execute() {
                      messageStore = new MessageStore() ;
                      return SUCCESS;
                  }
                  
                  public MessageStore getMessageStore() {
                      return messageStore;
                  }
              }
              """
          ),
          srcMainResources(
            xml(
              //language=xml
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE struts PUBLIC
                    "-//Apache Software Foundation//DTD Struts Configuration 2.5//EN"
                    "http://struts.apache.org/dtds/struts-2.5.dtd">
                <struts>
                    <constant name="struts.devMode" value="true" />
                              
                    <package name="basicstruts2" extends="struts-default">
                        <action name="index">
                            <result>/index.jsp</result>
                        </action>
                    
                        <action name="hello" class="org.apache.struts.helloworld.action.HelloWorldAction" method="execute">
                            <result name="success">/HelloWorld.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
              spec -> spec.path("struts.xml")
            )
          ),
          dir("src/main/webapp",
            text(
              """
                <!DOCTYPE html>
                <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
                <%@ taglib prefix="s" uri="/struts-tags" %>
                <html>
                  <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <title>Hello World!</title>
                  </head>
                  <body>
                    <h2><s:property value="messageStore.message" /></h2>
                  </body>
                </html>
                """,
              spec -> spec.path("index.jsp")
            )
          )
        );
    }
}
