select id, name, canonical_form, italicised_form, rank, status, position, cast(path as varchar), hierarchy, accepted_ids, accepted_names, common_names, group_id, group_name from
	(
		select id, name, canonical_form, italicised_form, rank, status, position
		from taxonomy_definition where is_deleted = false and id in (:taxonIds)
	) TD

	left outer join
		(select synonym_id, array_agg(accepted_id) as accepted_ids, array_agg(name) as accepted_names from
		(select synonym_id, accepted_id from accepted_synonym) A 
		inner join (select id, name from taxonomy_definition) TA on TA.id = A.accepted_id group by synonym_id
		) A 
	on TD.id = A.synonym_id

	left outer join
		(select taxon_definition_id as taxon_id, original_path as path,json_agg(row_to_json((select t from (select taxon_id, taxon_rank, taxon_name) t)) order by nr) as hierarchy from 
			(
			SELECT *, row_number() OVER(PARTITION by sid order by sid ) AS nr
			FROM  (SELECT id as sid, taxon_definition_id, p.split_path, path as original_path from taxonomy_registry T , 
			unnest(cast(string_to_array(ltree2text(path), '.') as integer[])) AS p(split_path) where T.classification_id = 1) tr_split
			) S
			inner join
			(select id taxon_id, rank taxon_rank, name as taxon_name from taxonomy_definition where is_deleted = false ) TD 
			on TD.taxon_id = S.split_path group by taxon_definition_id, original_path
		) TR
	on TD.id = TR.taxon_id

	left outer join
		(select taxon_concept_id, json_agg(row_to_json((SELECT t FROM (SELECT id ,name,language_id ,language_name,three_letter_code) t))) AS common_names
			from (
				select id, name , taxon_concept_id,language_id,language_name,three_letter_code from 
				(select id, name, language_id, taxon_concept_id from common_names where is_deleted = false and name !~ '^[0-9][0-9]|^[)`-]|^A$|^0$') CN 
				left outer join (select id l_id, name as language_name, three_letter_code from language) as L
				on CN.language_id = L.l_id 
			) CN GROUP BY taxon_concept_id
		) CN
	on CN.taxon_concept_id = TD.id

	left outer join
		(select taxon_definition_id, species_group_id group_id, name group_name from species_group sg inner join 
			(
				select t2.taxon_definition_id taxon_definition_id, (select species_group_id from species_group_mapping where taxon_concept_id = t1.taxon_definition_id)
				from (select * from taxonomy_registry t2 where t2.classification_id = 1 and t2.taxon_definition_id in (select taxon_concept_id from species_group_mapping)) t1
				inner join (select * from taxonomy_registry where classification_id = 1) t2 on t1.path @> t2.path
				) t on t.species_group_id = sg.id order by taxon_definition_id

		) SG
	on SG.taxon_definition_id = TD.id
