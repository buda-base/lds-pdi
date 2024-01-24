# ISBN search API

This endpoint takes as input any identifier of a book (ISBN, ISSN, EAN) and returns information about title, author, other identifiers and volumes.

### `searchByID?id={id}`

The HTTP header `Accept-Language` will have an impact on the results, set it to `bo` to get the Tibetan Unicode version.

where `{id}` is any identifier, the API is tailored to ISBNs, ISSNs and EANs.

There are 3 types of matches:
- `monovolume`: the match is a one volume edition
- `full_multivolume`: the match is a multi volume edition, all volumes could match
- `subset`: the match is a multi-volume edition, only some volumes match

Example of a `monovolume` match (`id=9789380359700`, `Accept-Language "bo"`):

```json
[
	{
		"id": "http://purl.bdrc.io/resource/MW1KG13995",
		"title": "བཞིན་བཟང་མ་གཏུམ་དྲག་རོལ་པའི་སྒྲུང་གཏམ།",
		"match_type": "monovolume",
		"author_name": "བློ་བཟང་རྒྱལ་མཚན།",
		"author_id": "http://purl.bdrc.io/resource/P9521",
		"nb_volumes": 1,
		"thumbnail_iiif_service": "https://iiif.bdrc.io/bdr:I1KG14309::I1KG143090003.jpg",
		"ids": ["9789380359700"]
	}
]
```

Example of a `subset` match (`?id=9787800579875`), matches the first 6 volumes of MW1KG14896 but not the last 6.

```json
[
	{
		"id": "http://purl.bdrc.io/resource/MW1KG14896",
		"title": "མང་ཚོགས་སྒྱུ་རྩལ།་ལེགས་རྩོམ་དཔེ་ཚོགས།",
		"nb_volumes": 12,
		"edition_statement": "པར་གཞི་དང་པོ།",
		"match_type": "subset",
		"thumbnail_iiif_service": "https://iiif.bdrc.io/bdr:I1KG14906::I1KG149060003.jpg",
		"ids": [],
		"volumes": [
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 1,
				"title": "Volume 1",
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14906"],
				"ids": ["9787800579875"]
			},
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 2,
				"title": "Volume 2",
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14907"],
				"ids": ["9787800579875"]
			},
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 3,
				"title": "Volume 3",
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14908"],
				"ids": ["9787800579875"]
			},
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 4,
				"title": "Volume 4",
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14909"],
				"ids": ["9787800579875"]
			},
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 5,
				"title": "Volume 5",
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14910"],
				"ids": ["9787800579875"]
			},
			{
				"id": "http://purl.bdrc.io/resource/MW1KG14896_O1KG14896_9D0E4TT10JYA",
				"volume_number": 6,
				"title": "Volume 6",
				"ids": ["9787800579875"],
				"image_groups": ["http://purl.bdrc.io/resource/I1KG14911"],
			}
		]
	}
]
```

