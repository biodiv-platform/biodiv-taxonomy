SELECT COUNT(DISTINCT(TD.ID))
FROM
	(SELECT *
		FROM TAXONOMY_REGISTRY
		WHERE PATH <@
				(SELECT PATH
					FROM TAXONOMY_REGISTRY
					WHERE TAXON_DEFINITION_ID = :taxonId
						AND CLASSIFICATION_ID = :classificationId)) TR
LEFT JOIN ACCEPTED_SYNONYM A ON TR.TAXON_DEFINITION_ID = A.ACCEPTED_ID
LEFT JOIN TAXONOMY_DEFINITION TD ON TD.ID = TR.TAXON_DEFINITION_ID
OR TD.ID = A.SYNONYM_ID
WHERE TD.RANK IN (:rank) AND STATUS IN (:status) AND POSITION IN (:position)