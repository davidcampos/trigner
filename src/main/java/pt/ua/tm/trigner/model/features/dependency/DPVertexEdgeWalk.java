package pt.ua.tm.trigner.model.features.dependency;

import org.jgrapht.Graph;
import org.jgrapht.alg.BellmanFordShortestPath;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.corpus.dependency.LabeledEdge;
import pt.ua.tm.gimli.features.corpus.pipeline.FeatureExtractor;
import pt.ua.tm.trigner.shared.Types;
import pt.ua.tm.trigner.shared.Types.VertexFeatureType;
import pt.ua.tm.trigner.util.TokenFeatureUtil;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 12/03/13
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
public class DPVertexEdgeWalk implements FeatureExtractor {

    private int maxHops;
    private String prefix;
    private Types.VertexFeatureType feature;

    public DPVertexEdgeWalk(final String prefix, final Types.VertexFeatureType feature, final int maxHops) {
        this.prefix = prefix;
        this.maxHops = maxHops;
        this.feature = feature;
    }

    @Override
    public void extract(Sentence sentence) {
        Graph graph = sentence.getDependencyGraph();
        for (Token token1 : sentence) {
            BellmanFordShortestPath<Token, LabeledEdge> bellman =
                    new BellmanFordShortestPath<Token, LabeledEdge>(graph, token1, maxHops);

            for (Token token2 : sentence) {
                if (token1.equals(token2)) {
                    continue;
                }
                List<LabeledEdge> edges = bellman.getPathEdgeList(token2);
                if (edges == null) {
                    continue;
                }
                if (edges.size() < maxHops) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();

                sb.append(TokenFeatureUtil.getFeature(token1, feature));
                sb.append("-");

                Token previous = token1;
                for (LabeledEdge edge : edges) {

                    sb.append(edge.getLabel());
                    sb.append("-");

                    Token token3;
                    if (edge.getV1().equals(previous)) {
                        token3 = (Token) edge.getV2();
                    } else {
                        token3 = (Token) edge.getV1();
                    }


                    sb.append(TokenFeatureUtil.getFeature(token3, feature));
                    sb.append("-");

                    previous = token3;
                }

                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }
                token1.putFeature(prefix, sb.toString());
            }

        }
    }
}
