package org.hyperic.hq.hqapi1.tools;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.hyperic.hq.hqapi1.AlertDefinitionApi;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.XmlUtil;
import org.hyperic.hq.hqapi1.GroupApi;
import org.hyperic.hq.hqapi1.types.AlertDefinition;
import org.hyperic.hq.hqapi1.types.AlertDefinitionsResponse;
import org.hyperic.hq.hqapi1.types.StatusResponse;
import org.hyperic.hq.hqapi1.types.GroupResponse;
import org.hyperic.hq.hqapi1.types.Resource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AlertDefinitionCommand extends Command {

    private static String CMD_LIST   = "list";
    private static String CMD_SYNC   = "sync";
    private static String CMD_DELETE = "delete";

    private static String[] COMMANDS = { CMD_LIST, CMD_DELETE, CMD_SYNC };

    private static String OPT_TYPEALERTS = "typeAlerts";
    private static String OPT_EXCLUDE_TYPEALERTS = "excludeTypeAlerts";
    private static String OPT_EXCLUDE_IDS = "excludeTypeIds";
    private static String OPT_GROUP = "group";
    private static String OPT_RESOURCE_NAME = "resourceName";
    private static String OPT_ID   = "id";

    private void printUsage() {
        System.err.println("One of " + Arrays.toString(COMMANDS) + " required");
    }

    protected void handleCommand(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        if (args[0].equals(CMD_LIST)) {
            list(trim(args));
        } else if (args[0].equals(CMD_DELETE)) {
            delete(trim(args));
        } else if (args[0].equals(CMD_SYNC)) {
            sync(trim(args));
        } else {
            printUsage();
            System.exit(-1);
        }
    }

    private void list(String[] args) throws Exception {

        OptionParser p = getOptionParser();

        p.accepts(OPT_TYPEALERTS, "If specified, only parent resource type " +
                                  "alerts will be returned.");
        p.accepts(OPT_EXCLUDE_TYPEALERTS, "If specified, individual alerts " +
                                          "based on resource type alerts will " +
                                          "be excluded.");
        p.accepts(OPT_EXCLUDE_IDS, "If specified, parent alert definitions will " +
                                   "not include alert ids.");
        p.accepts(OPT_GROUP, "If specified, only show alert definitions for " +
                             "resources that belong to the specified group.").
                withRequiredArg().ofType(String.class);
        p.accepts(OPT_RESOURCE_NAME, "If specified, only show alert definitions " +
                                     "belonging to a resource with the given " +
                                     "resource name regex.").
                withRequiredArg().ofType(String.class);

        OptionSet options = getOptions(p, args);

        HQApi api = getApi(options);
        AlertDefinitionApi definitionApi = api.getAlertDefinitionApi();
        GroupApi groupApi = api.getGroupApi();

        AlertDefinitionsResponse alertDefs;
        
        if (options.has(OPT_TYPEALERTS)) {
            boolean excludeIds = false;
            if (options.has(OPT_EXCLUDE_IDS)) {
                excludeIds = true;
            }
            
            alertDefs = definitionApi.getTypeAlertDefinitions(excludeIds);
        } else {
            boolean excludeTypeAlerts = false;
            if (options.has(OPT_EXCLUDE_TYPEALERTS)) {
                excludeTypeAlerts = true;
            }
            if (options.has(OPT_EXCLUDE_IDS)) {
                System.err.println("Option " + OPT_EXCLUDE_IDS + " only valid " +
                                   " when " + OPT_TYPEALERTS + " is specified.");
                System.exit(-1);
            }
            alertDefs = definitionApi.getAlertDefinitions(excludeTypeAlerts);
        }

        // Filter on group if necessary
        if (options.has(OPT_GROUP)) {
            GroupResponse group =
                    groupApi.getGroup((String)options.valueOf(OPT_GROUP));
            checkSuccess(group);

            List<Integer> includedResources = new ArrayList<Integer>();
            for (Resource r : group.getGroup().getResource()) {
                includedResources.add(r.getId());
            }

            for (Iterator<AlertDefinition> i = alertDefs.getAlertDefinition().iterator();
                 i.hasNext(); ) {
                AlertDefinition d = i.next();
                Integer rid = d.getResource().getId();
                if (!includedResources.contains(rid)) {
                    System.out.println("Removing def " + d.getId());
                    i.remove();
                }
            }
        }

        // Filter on resource name if necessary
        if (options.has(OPT_RESOURCE_NAME)) {
            Pattern pattern = Pattern.compile((String)options.valueOf(OPT_RESOURCE_NAME));

            for (Iterator<AlertDefinition> i = alertDefs.getAlertDefinition().iterator();
                 i.hasNext(); ) {
                AlertDefinition d = i.next();
                Matcher m = pattern.matcher(d.getResource().getName());
                if (!m.matches()) {
                    i.remove();
                }
            }
        }

        checkSuccess(alertDefs);
        XmlUtil.serialize(alertDefs, System.out, Boolean.TRUE);        
    }

    private void delete(String[] args) throws Exception {

        OptionParser p = getOptionParser();

        p.accepts(OPT_ID, "The alert definition id to delete").
                withRequiredArg().ofType(Integer.class);

        OptionSet options = getOptions(p, args);

        HQApi api = getApi(options);
        AlertDefinitionApi definitionApi = api.getAlertDefinitionApi();

        Integer id = (Integer)getRequired(options, OPT_ID);
        StatusResponse deleteResponse =
                definitionApi.deleteAlertDefinition(id);
        checkSuccess(deleteResponse);

        System.out.println("Successfully deleted alert definition id " + id);
    }

    private void sync(String[] args) throws Exception {

        OptionParser p = getOptionParser();
        OptionSet options = getOptions(p, args);

        AlertDefinitionApi api = getApi(options).getAlertDefinitionApi();

        InputStream is = getInputStream(options);

        AlertDefinitionsResponse resp =
                XmlUtil.deserialize(AlertDefinitionsResponse.class, is);
        
        List<AlertDefinition> definitions = resp.getAlertDefinition();

        AlertDefinitionsResponse syncResponse = api.syncAlertDefinitions(definitions);
        checkSuccess(syncResponse);

        System.out.println("Successfully synced " + definitions.size() +
                           " alert definitions.");
    }
}
