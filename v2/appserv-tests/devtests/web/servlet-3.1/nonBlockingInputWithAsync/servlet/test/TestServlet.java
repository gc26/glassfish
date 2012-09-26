/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package test;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns="/test", asyncSupported=true)
public class TestServlet extends HttpServlet {

    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        AsyncContext ac = req.startAsync();
        //ac.setTimeout(-1);
        ServletOutputStream output = res.getOutputStream();
        ServletInputStream input = req.getInputStream();
        ReadListenerImpl readListener = new ReadListenerImpl(input, output, ac);
        input.setReadListener(readListener);

        int b = -1;
        while (input.isReady() && ((b = input.read()) != -1)) {
            System.out.print((char)b);
            output.write(b);
        }

        if (input.isFinished()) {
            ac.complete();
        }
    }

    static class ReadListenerImpl implements ReadListener {
        private ServletInputStream input = null;
        private ServletOutputStream output = null;
        private AsyncContext ac = null;

        ReadListenerImpl(ServletInputStream in, ServletOutputStream out,
                AsyncContext c) {
            input = in;
            output = out;
            ac = c;
        }

        public void onDataAvailable() {
            try {
                StringBuilder sb = new StringBuilder();
                System.out.println("--> onDataAvailable");
                int len = -1;
                byte b[] = new byte[1024];
                while (input.isReady() 
                        && (len = input.read(b)) != -1) {
                    String data = new String(b, 0, len);
                    System.out.println("--> " + data);
                    sb.append('/' + data);
                    //output.print('/' + data);
                }
                output.print(sb.toString());
            } catch(Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void onAllDataRead() {
            try {
                System.out.println("--> onAllDataRead");
                output.println("-onAllDataRead");
            } catch(Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                ac.complete();
            }
        }

        public void onError(final Throwable t) {
            ac.complete();
            t.printStackTrace();
        }
    }
}