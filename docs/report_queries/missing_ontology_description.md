# Missing Ontology Description

**Problem:** The ontology header is missing required metadata: [description](http://dublincore.org/documents/dcmi-terms/#elements-description).

**Solution:** Add the missing description. See link for appropriate property.

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT DISTINCT ?entity ?property ?value WHERE {
 VALUES ?property { dc:description }
 ?entity a owl:Ontology .
 OPTIONAL { ?entity ?property ?value }
 FILTER (!bound(?value))
}
```
