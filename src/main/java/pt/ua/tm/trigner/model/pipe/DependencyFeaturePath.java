package pt.ua.tm.trigner.model.pipe;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 05/03/13
 * Time: 14:00
 * To change this template use File | Settings | File Templates.
 */
public class DependencyFeaturePath extends Pipe {

    private int length;
    private String prefix;
    private Pattern featureRegex;

    public DependencyFeaturePath(final String prefix, final int length, final Pattern featureRegex) {
        this.prefix = prefix;
        this.length = length;
        this.featureRegex = featureRegex;
    }

    @Override
    public Instance pipe(Instance carrier) {
        TokenSequence ts = (TokenSequence) carrier.getData();
        List<Dependency> dependencies = Dependency.getDependencies(ts);

        for (int i = 0; i < ts.size(); i++) {
            Token token = ts.get(i);

            String path = getPath(dependencies, ts, i, length);

            if (path != null && !path.isEmpty()) {
                token.setFeatureValue(prefix + "=" + path, 1.0);
            }
        }

        return carrier;
    }

    private String getPath(List<Dependency> dependencies, final TokenSequence tokenSequence, final int tokenPos,
                           final int length) {
        StringBuilder sb = new StringBuilder();

        Dependency dependency = dependencies.get(tokenPos);

        int pathLength = 0;

        for (int current = 0; current < length; current++) {
            if (dependency.getPos() == -1) {
                break;
            }

            Token token = tokenSequence.get(dependency.getPos());
            String feature = TokenUtil.getFeature(token, featureRegex);
            feature = feature.substring(feature.indexOf("=") + 1);
            if (feature != null) {
                sb.append(feature);
                sb.append("->");
            }

            dependency = dependencies.get(dependency.getPos());
            pathLength++;
        }

        if (pathLength < length) {
            return null;
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }
}
