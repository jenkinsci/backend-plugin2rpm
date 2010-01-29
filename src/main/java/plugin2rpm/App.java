package plugin2rpm;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.update_center.HPI;
import org.jvnet.hudson.update_center.HPI.Dependency;
import org.jvnet.hudson.update_center.MavenRepository;
import org.jvnet.hudson.update_center.PluginHistory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        MavenRepository repository = new MavenRepository("java.net2",new URL("http://maven.dyndns.org/2/"));

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(App.class,"");
        DefaultObjectWrapper ow = new DefaultObjectWrapper();
        ow.setExposeFields(true);
        cfg.setObjectWrapper(ow);

        Template template = cfg.getTemplate("specfile.ftl");

        File baseDir = new File("./target").getAbsoluteFile();
        File rpms = new File(baseDir,"RPMS");
        rpms.mkdirs();
        File srpms = new File(baseDir,"SRPMS");
        srpms.mkdirs();

        for( PluginHistory hpi : repository.listHudsonPlugins() ) {
            System.out.println("=================================="+hpi.artifactId);

            List<HPI> versions = new ArrayList<HPI>(hpi.artifacts.values());
            HPI latest = versions.get(0);
//            HPI previous = versions.size()>1 ? versions.get(1) : null;

            // compute plugin dependencies
            StringBuilder buf = new StringBuilder();
            for (Dependency d : latest.getDependencies()) {
                if (d.optional) continue;
                if (buf.length()>0) buf.append(' ');
                buf.append(asPluginName(d.name)).append(" >= ").append(normalizeVersion(d.version));
            }

            Map others = new HashMap();
            others.put("name",asPluginName(hpi.artifactId));
            others.put("dependencies",buf.toString());
            others.put("version",normalizeVersion(latest.version));

            File spec = new File(baseDir, "spec");
            FileWriter out = new FileWriter(spec);
            template.process(new UnionHashModel(
                    (TemplateHashModel)ow.wrap(others),
                    (TemplateHashModel)ow.wrap(latest)), out);
            out.close();

            recreate(new File(baseDir,"BUILDROOT"));
            File sources = recreate(new File(baseDir,"SOURCES"));
            recreate(new File(baseDir,"BUILD"));

            // rpmbuild wants everything in the SOURCES dir.
            FileUtils.copyFile(latest.resolve(),new File(sources,hpi.artifactId+".hpi"));

            ProcessBuilder pb = new ProcessBuilder("rpmbuild", "-ba", "--define=_topdir " + baseDir, "--define=_tmppath " + baseDir + "/tmp", spec.getPath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getOutputStream().close();
            IOUtils.copy(p.getInputStream(),System.out);
            int r = p.waitFor();
            if (r!=0)
                return;
        }
    }

    private static String asPluginName(String name) {
        return "hudson-"+name +"-plugin";
    }

    private static File recreate(File sources) throws IOException {
        FileUtils.deleteDirectory(sources);
        sources.mkdirs();
        return sources;
    }

    /**
     * Anything goes in Maven version, but not so in RPM.
     */
    private static String normalizeVersion(String v) {
        return v.replace('-','.');
    }
}
