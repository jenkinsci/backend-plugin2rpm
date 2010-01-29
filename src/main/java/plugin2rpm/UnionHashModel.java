package plugin2rpm;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author Kohsuke Kawaguchi
 */
class UnionHashModel implements TemplateHashModel {
    private final TemplateHashModel lhs, rhs;

    public UnionHashModel(TemplateHashModel lhs, TemplateHashModel rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public boolean isEmpty() throws TemplateModelException {
        return lhs.isEmpty() && rhs.isEmpty();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        TemplateModel o = lhs.get(key);
        if (o==null)    o=rhs.get(key);
        return o;
    }
}
