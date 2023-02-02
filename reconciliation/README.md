# Reconciling data with BDRC

BDRC implements the [Reconciliation API](https://reconciliation-api.github.io/specs/0.1/) and can now be used in the well known [OpenRefine](https://openrefine.org/).

### Configuration

BDRC's service URL is `https://ldspdi.bdrc.io/reconciliation/en/`, see [OpenRefine's documentation](https://openrefine.org/docs/manual/reconciling) on how to configure and use it there.

The currently supported types are `Person` and `Work`.

Two properties can be used for [reconciling with additional columns](https://openrefine.org/docs/manual/reconciling#reconciling-with-additional-columns):
- `authorOf` can be used when reconciling persons to indicate the work title or work id
- `hasAuthor` can be used when reconciling works to indicate the author's name or id

The results are returned in Extended Wylie (EWTS), the names are expected to be either in Extended Wylie or Tibetan Unicode.

The results are sorted according to their popularity index, a value calculated by looking at connections in the BDRC database (number of authored works, number of commentaries of these works, etc.).

The reconciliation queries our database using our [Lucene Tibetan Analyzer](https://github.com/buda-base/lucene-bo/) (see these blog posts: [[1]](https://www.bdrc.io/blog/2014/05/02/tibetan-search-enhancements/) [[2]](https://www.bdrc.io/blog/2021/01/26/tibetan-search-enhancements-part-2/) for some of the features). It also performs additional normalization that removes some titles (such as *mkhan po*, *rin po che*, etc.) that are often found in name list but not always recorded in our database, and would impede reconciliation.

### Example

This folder contains an [example CSV file](example.csv) that can be used to test the API.

To load it in OpenRefine and configure the BDRC API:
- launch OpenRefine
- in the first page, open the example.csv file
- click on `create project` in the top right corner
- click on the arrow next to the "authors" cell -> `Reconcile` -> `Start reconciling`
- click on `Add standard service` in the bottom left corner of the pop-up window
- enter the url `https://ldspdi.bdrc.io/reconciliation/en/` and click on `Add service`

### Tips

The reconciliation of a list of authors + work titles can be done in several steps:
- first reconciling the authors taking the works into account, this will usually leave a lot of unreconciled data
- reconciling the works taking the authors into account
- reconciling the authors taking the works into account again, this will have a slightly different effect since it will add the authors of the reconciled works to the list of candidates. Our experience is that this is a significant gain
