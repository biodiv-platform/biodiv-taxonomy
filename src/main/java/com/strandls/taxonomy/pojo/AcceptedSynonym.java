/**
 * 
 */
package com.strandls.taxonomy.pojo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Abhishek Rudra
 *
 */

@NamedQuery(name = "synonymTransfer", query = "update AcceptedSynonym set acceptedId = :newAcceptedId where acceptedId = :acceptedId") 
@Entity
@Table(name = "accepted_synonym")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcceptedSynonym implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long version;
	private Long acceptedId;
	private Long synonymId;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accepted_synonym_id_generator")
	@SequenceGenerator(name = "accepted_synonym_id_generator", sequenceName = "accepted_synonym_id_seq", allocationSize = 1)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Column(name = "accepted_id")
	public Long getAcceptedId() {
		return acceptedId;
	}

	public void setAcceptedId(Long acceptedId) {
		this.acceptedId = acceptedId;
	}

	@Column(name = "synonym_id")
	public Long getSynonymId() {
		return synonymId;
	}

	public void setSynonymId(Long synonymId) {
		this.synonymId = synonymId;
	}

}
