/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.ingestion.morphline.detector;

import com.stratio.ingestion.detector.source.http.handler.DetectorJsonHandler;
import com.typesafe.config.Config;
import org.apache.flume.Event;
import org.apache.log4j.Logger;
import org.kitesdk.morphline.api.Command;
import org.kitesdk.morphline.api.CommandBuilder;
import org.kitesdk.morphline.api.MorphlineContext;
import org.kitesdk.morphline.api.Record;
import org.kitesdk.morphline.base.AbstractCommand;

import java.util.*;

/**
 * Builder for parsing Grupo Detector bulk-loaded events.
 */
public class DetectorBuilder implements CommandBuilder {

    private final static Logger LOG = Logger.getLogger(DetectorBuilder.class);

    private final static String COMMAND_NAME = "parseDetectorLine";

    private static DetectorJsonHandler jsonHandler = new DetectorJsonHandler();

    @Override
    public Collection<String> getNames() {
        return Collections.singletonList(COMMAND_NAME);
    }

    @Override
    public Command build(Config config, Command parent, Command child, MorphlineContext context) {
        return new DetectorFrame(this, config, parent, child, context);
    }

    private static final class DetectorFrame extends AbstractCommand {

        public DetectorFrame(CommandBuilder builder, Config config, Command parent, Command child, final MorphlineContext context) {
            super(builder, config, parent, child, context);
        }

        @Override
        protected boolean doProcess(Record record) {
            String line = (String)(record.get("message").get(0));
            String [] fields = line.split("\\|");
            String json = fields[fields.length -1];
            try {
                List<Event> events = jsonHandler.getEvents(json);
                record.removeAll("Value");
                if(!events.isEmpty()) {
                    Event event = events.get(0);
                    Map<String, String> headers = event.getHeaders();
                    for (Map.Entry<String, String> header: headers.entrySet()) {
                        record.put(header.getKey(), header.getValue());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error while parsing event: " + json, e);
                return false;
            }
            return super.doProcess(record);
        }
    }

}
