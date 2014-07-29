package eurekademo;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.*;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.StatusResource;
import com.netflix.eureka.util.StatusInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by sgibb on 7/29/14.
 */
@Controller
public class EurekaController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String status(HttpServletRequest request, Map<String, Object> model) {
        String path = request.getContextPath();
        String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";

        model.put("time", new Date());
        model.put("basePath", basePath);

        List<com.netflix.discovery.shared.Application> sortedApplications = PeerAwareInstanceRegistry.getInstance().getSortedApplications();

        ArrayList<Map<String, Object>> apps = new ArrayList<>();

        for(com.netflix.discovery.shared.Application app : sortedApplications) {
            LinkedHashMap<String, Object> appData = new LinkedHashMap<>();
            apps.add(appData);

            appData.put("name", app.getName());
            Map<String, Integer> amiCounts = new HashMap<>();
            Map<InstanceInfo.InstanceStatus,List<Pair<String, String>>> instancesByStatus = new HashMap<>();
            Map<String,Integer> zoneCounts = new HashMap<>();

            for(InstanceInfo info : app.getInstances()){
                String id = info.getId();
                String url = info.getStatusPageUrl();
                InstanceInfo.InstanceStatus status = info.getStatus();
                String ami = "n/a";
                String zone = "";
                if(info.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon){
                    AmazonInfo dcInfo = (AmazonInfo)info.getDataCenterInfo();
                    ami = dcInfo.get(AmazonInfo.MetaDataKey.amiId);
                    zone = dcInfo.get(AmazonInfo.MetaDataKey.availabilityZone);
                }

                Integer count = amiCounts.get(ami);
                if(count != null){
                    amiCounts.put(ami, Integer.valueOf(count.intValue()+1));
                }else {
                    amiCounts.put(ami, Integer.valueOf(1));
                }

                count = zoneCounts.get(zone);
                if(count != null){
                    zoneCounts.put(zone, Integer.valueOf(count.intValue()+1));
                }else {
                    zoneCounts.put(zone, Integer.valueOf(1));
                }
                List<Pair<String, String>> list = instancesByStatus.get(status);

                if(list == null){
                    list = new ArrayList<>();
                    instancesByStatus.put(status, list);
                }
                list.add(new Pair<>(id, url));
            }

            appData.put("amiCounts", amiCounts.entrySet());
            appData.put("zoneCounts", zoneCounts.entrySet());

            ArrayList<Map<String, Object>> instanceInfos = new ArrayList<>();
            appData.put("instanceInfos", instanceInfos);

            for (Iterator<Map.Entry<InstanceInfo.InstanceStatus, List<Pair<String,String>>>> iter =
                         instancesByStatus.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<InstanceInfo.InstanceStatus, List<Pair<String,String>>> entry = iter.next();
                List<Pair<String, String>> value = entry.getValue();
                InstanceInfo.InstanceStatus status = entry.getKey();

                LinkedHashMap<String, Object> instanceData = new LinkedHashMap<>();
                instanceInfos.add(instanceData);

                instanceData.put("status", entry.getKey());
                ArrayList<Map<String, Object>> instances = new ArrayList<>();
                instanceData.put("instances", instances);
                instanceData.put("isNotUp", status != InstanceInfo.InstanceStatus.UP);

                /*if(status != InstanceInfo.InstanceStatus.UP){
                    buf.append("<font color=red size=+1><b>");
                }
                buf.append("<b>").append(status.name()).append("</b> (").append(value.size()).append(") - ");
                if(status != InstanceInfo.InstanceStatus.UP){
                    buf.append("</font></b>");
                }*/

                for(Pair<String,String> p : value) {
                    LinkedHashMap<String, Object> instance = new LinkedHashMap<>();
                    instances.add(instance);
                    instance.put("id", p.first());
                    instance.put("url", p.second());
                    instance.put("isHref", p.second().startsWith("http"));
                    /*String id = p.first();
                    String url = p.second();
                    if(url != null && url.startsWith("http")){
                        buf.append("<a href=\"").append(url).append("\">");
                    }else {
                        url = null;
                    }
                    buf.append(id);
                    if(url != null){
                        buf.append("</a>");
                    }
                    buf.append(", ");*/
                }
            }
            //out.println("<td>" + buf.toString() + "</td></tr>");
        }

        model.put("apps", apps);

        StatusInfo statusInfo = (new StatusResource()).getStatusInfo();
        model.put("statusInfo", statusInfo);

        InstanceInfo instanceInfo = statusInfo.getInstanceInfo();

        Map<String,String> instanceMap = new HashMap<>();
        instanceMap.put("ipAddr", instanceInfo.getIPAddr());
        instanceMap.put("status", instanceInfo.getStatus().toString());
        if(instanceInfo.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon) {
            AmazonInfo info = (AmazonInfo) instanceInfo.getDataCenterInfo();
            instanceMap.put("availability-zone", info.get(AmazonInfo.MetaDataKey.availabilityZone));
            instanceMap.put("public-ipv4", info.get(AmazonInfo.MetaDataKey.publicIpv4));
            instanceMap.put("instance-id", info.get(AmazonInfo.MetaDataKey.instanceId));
            instanceMap.put("public-hostname", info.get(AmazonInfo.MetaDataKey.publicHostname));
            instanceMap.put("ami-id", info.get(AmazonInfo.MetaDataKey.amiId));
            instanceMap.put("instance-type", info.get(AmazonInfo.MetaDataKey.instanceType));
        }

        model.put("instanceInfo", instanceMap);

        return "status";
    }
}
