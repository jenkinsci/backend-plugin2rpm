package plugin2rpm;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
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
    private final MavenRepository repository;
    public File baseDir = new File("./target").getAbsoluteFile();

    public App(MavenRepository repository) {
        this.repository = repository;
    }

    public App() throws Exception {
        this(new MavenRepository("java.net2",new URL("http://maven.dyndns.org/2/")));
    }

    public static void main(String[] args) throws Exception {
        System.exit(new App().run());
    }

    public int run() throws Exception {
        MavenRepository repository = new MavenRepository("java.net2",new URL("http://maven.dyndns.org/2/"));

        Template template = loadTemplate();

        File rpms = new File(baseDir,"RPMS");
        rpms.mkdirs();
        File srpms = new File(baseDir,"SRPMS");
        srpms.mkdirs();

        for( PluginHistory hpi : repository.listHudsonPlugins() ) {
            System.out.println("=================================="+hpi.artifactId);

            List<HPI> versions = new ArrayList<HPI>(hpi.artifacts.values());
            HPI latest = versions.get(0);
//            HPI previous = versions.size()>1 ? versions.get(1) : null;

            File spec = new File(baseDir, hpi.artifactId+".spec");
            FileWriter out = new FileWriter(spec);
            template.process(buildModel(hpi, latest), out);
            out.close();

            recreate(new File(baseDir,"BUILDROOT"));
            File sources = recreate(new File(baseDir,"SOURCES"));
            recreate(new File(baseDir,"BUILD"));

            // rpmbuild wants everything in the SOURCES dir.
            FileUtils.copyFile(latest.resolve(),new File(sources,hpi.artifactId+".hpi"));

            int r = execAndWait(new ProcessBuilder("rpmbuild", "-ba", "--define=_topdir " + baseDir, "--define=_tmppath " + baseDir + "/tmp", spec.getPath()));
            if (r!=0)
                return r;
        }
        return 0;
    }

    private UnionHashModel buildModel(PluginHistory hpi, HPI latest) throws IOException, TemplateModelException {
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
        others.put("it",latest);

        DefaultObjectWrapper ow = new DefaultObjectWrapper();
        ow.setExposeFields(true);
        ow.setNullModel(TemplateModel.NOTHING);
        return new UnionHashModel(
                (TemplateHashModel) ow.wrap(others),
                (TemplateHashModel) ow.wrap(latest));
    }

    protected Template loadTemplate() throws IOException {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(App.class,"");

        return cfg.getTemplate("specfile.ftl");
    }

    private int execAndWait(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().close();
        IOUtils.copy(p.getInputStream(),System.out);
        return p.waitFor();
    }

    private static String asPluginName(String name) {
        return "hudson-"+name +"-plugin";
    }

    private File recreate(File sources) throws IOException {
        FileUtils.deleteDirectory(sources);
        sources.mkdirs();
        return sources;
    }

    /**
     * Anything goes in Maven version, but not so in RPM.
     */
    private String normalizeVersion(String v) {
        return v.replace('-','.');
    }
}
