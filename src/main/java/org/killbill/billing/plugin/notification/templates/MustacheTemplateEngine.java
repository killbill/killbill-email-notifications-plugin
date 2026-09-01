/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.notification.templates;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

public class MustacheTemplateEngine implements TemplateEngine {

    private static final Mustache.TemplateLoader PARTIAL_LOADER = name -> {
        final String resourcePath = "org/killbill/billing/plugin/notification/templates/" + name + ".mustache";
        final URL url = MustacheTemplateEngine.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IOException("Unable to find partial template: " + resourcePath);
        }
        return new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
    };
    private static final Mustache.Compiler COMPILER =
            Mustache.compiler().nullValue("").withLoader(PARTIAL_LOADER);

    @Override
    public String executeTemplateText(final String templateText, final Map<String, Object> data) {
        final Template template = COMPILER.compile(templateText);
        return template.execute(data);
    }
}
