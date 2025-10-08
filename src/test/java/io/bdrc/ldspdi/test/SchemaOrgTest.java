package io.bdrc.ldspdi.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ldspdi.export.SchemaOrgJsonLDExport;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class SchemaOrgTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void test_MW12827_jsonld_matches_expected() throws Exception {
        Path ttlPath = resolvePath("MW12827-so.ttl");
        Path expectedJsonPath = resolvePath("MW12827-so.jsonld");

        // 1) Load RDF (TTL) into Jena
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, ttlPath.toUri().toString(), Lang.TURTLE);

        // 2) Locate the main resource (MW12827)
        String uri = "http://purl.bdrc.io/resource/MW12827";
        Resource main = model.getResource(uri);

        // 3) Produce JSON-LD via your exporter
        JsonNode actual = SchemaOrgJsonLDExport.getObject(model, main); // static; adjust if your method is instance-based

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
        // 4) Load expected JSON-LD
        JsonNode expected = MAPPER.readTree(Files.readAllBytes(expectedJsonPath));
        

        // 5) Canonicalize both and compare
        JsonNode canExpected = canonicalize(expected);
        JsonNode canActual   = canonicalize(actual);

        if (!canExpected.equals(canActual)) {
            String msg = diffMessage(canExpected, canActual);
            Assertions.fail("Actual JSON-LD does not match expected.\n" + msg);
        }
    }

    @Test
    void test_P1583_jsonld_matches_expected() throws Exception {
        Path ttlPath = resolvePath("MW12827-so.ttl");
        Path expectedJsonPath = resolvePath("P1583-so.jsonld");

        // 1) Load RDF (TTL) into Jena
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, ttlPath.toUri().toString(), Lang.TURTLE);

        // 2) Locate the main resource (MW12827)
        String uri = "http://purl.bdrc.io/resource/P1583";
        Resource main = model.getResource(uri);

        // 3) Produce JSON-LD via your exporter
        JsonNode actual = SchemaOrgJsonLDExport.getObject(model, main); // static; adjust if your method is instance-based

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
        // 4) Load expected JSON-LD
        JsonNode expected = MAPPER.readTree(Files.readAllBytes(expectedJsonPath));
        

        // 5) Canonicalize both and compare
        JsonNode canExpected = canonicalize(expected);
        JsonNode canActual   = canonicalize(actual);

        if (!canExpected.equals(canActual)) {
            String msg = diffMessage(canExpected, canActual);
            Assertions.fail("Actual JSON-LD does not match expected.\n" + msg);
        }
    }

    @Test
    void test_G36_jsonld_matches_expected() throws Exception {
        Path ttlPath = resolvePath("MW12827-so.ttl");
        Path expectedJsonPath = resolvePath("G36-so.jsonld");

        // 1) Load RDF (TTL) into Jena
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, ttlPath.toUri().toString(), Lang.TURTLE);

        // 2) Locate the main resource (MW12827)
        String uri = "http://purl.bdrc.io/resource/G36";
        Resource main = model.getResource(uri);

        // 3) Produce JSON-LD via your exporter
        JsonNode actual = SchemaOrgJsonLDExport.getObject(model, main); // static; adjust if your method is instance-based

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actual));
        // 4) Load expected JSON-LD
        JsonNode expected = MAPPER.readTree(Files.readAllBytes(expectedJsonPath));
        

        // 5) Canonicalize both and compare
        JsonNode canExpected = canonicalize(expected);
        JsonNode canActual   = canonicalize(actual);

        if (!canExpected.equals(canActual)) {
            String msg = diffMessage(canExpected, canActual);
            Assertions.fail("Actual JSON-LD does not match expected.\n" + msg);
        }
    }
    
    /* -----------------------------------------------------------
       Helpers
       ----------------------------------------------------------- */

    private static Path resolvePath(String fileName) {
        // Prefer src/test/resources
        Path inResources = Paths.get("src", "test", "resources", fileName);
        if (Files.exists(inResources)) return inResources;

        throw new IllegalStateException("Test input not found as " + inResources);
    }

    /**
     * Canonicalize a JSON node:
     * - Sort all object keys lexicographically.
     * - For arrays of objects, sort elements by @id, then url, then name when present.
     * - Other arrays keep their order.
     */
    private static JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isNull()) return node;

        if (node.isObject()) {
            ObjectNode src = (ObjectNode) node;
            ObjectNode dst = MAPPER.createObjectNode();
            // sort fields by key
            List<String> names = new ArrayList<>();
            src.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String k : names) {
                dst.set(k, canonicalize(src.get(k)));
            }
            return dst;
        } else if (node.isArray()) {
            ArrayNode src = (ArrayNode) node;
            // if array of objects, copy & sort by @id/url/name if available
            boolean allObjects = true;
            for (JsonNode n : src) {
                if (!n.isObject()) { allObjects = false; break; }
            }
            if (allObjects) {
                List<JsonNode> items = new ArrayList<>();
                src.forEach(items::add);
                items.sort(Comparator.comparing(SchemaOrgTest::keyForSort, Comparator.nullsLast(String::compareTo)));
                ArrayNode dst = MAPPER.createArrayNode();
                for (JsonNode it : items) dst.add(canonicalize(it));
                return dst;
            } else {
                // keep order, but canonicalize elements
                ArrayNode dst = MAPPER.createArrayNode();
                for (JsonNode n : src) dst.add(canonicalize(n));
                return dst;
            }
        } else {
            return node; // primitives unchanged
        }
    }

    private static String keyForSort(JsonNode obj) {
        if (obj == null || !obj.isObject()) return null;
        JsonNode id  = obj.get("@id");
        if (id != null && id.isTextual()) return "0|" + id.asText();
        JsonNode url = obj.get("url");
        if (url != null && url.isTextual()) return "1|" + url.asText();
        JsonNode name = obj.get("name");
        if (name != null && name.isTextual()) return "2|" + name.asText();
        return "9|" + obj.toString();
    }

    private static String diffMessage(JsonNode expected, JsonNode actual) {
        try {
            String exp = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(expected);
            String act = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actual);
            return "=== Expected (canonical) ===\n" + exp + "\n\n=== Actual (canonical) ===\n" + act + "\n";
        } catch (JsonProcessingException e) {
            return "Unable to pretty-print diff: " + e.getMessage();
        }
    }
}
