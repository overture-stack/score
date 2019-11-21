--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.12
-- Dumped by pg_dump version 9.6.12

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: access_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.access_type AS ENUM (
    'controlled',
    'open'
);


ALTER TYPE public.access_type OWNER TO postgres;

--
-- Name: analysis_state; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.analysis_state AS ENUM (
    'PUBLISHED',
    'UNPUBLISHED',
    'SUPPRESSED'
);


ALTER TYPE public.analysis_state OWNER TO postgres;

--
-- Name: analysis_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.analysis_type AS ENUM (
    'sequencingRead',
    'variantCall',
    'MAF'
);


ALTER TYPE public.analysis_type OWNER TO postgres;

--
-- Name: file_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.file_type AS ENUM (
    'FASTA',
    'FAI',
    'FASTQ',
    'BAM',
    'BAI',
    'VCF',
    'TBI',
    'IDX',
    'XML',
    'TGZ'
);


ALTER TYPE public.file_type OWNER TO postgres;

--
-- Name: gender; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.gender AS ENUM (
    'male',
    'female',
    'unspecified'
);


ALTER TYPE public.gender OWNER TO postgres;

--
-- Name: id_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.id_type AS ENUM (
    'Study',
    'Donor',
    'Specimen',
    'Sample',
    'File',
    'Analysis',
    'SequencingRead',
    'VariantCall'
);


ALTER TYPE public.id_type OWNER TO postgres;

--
-- Name: library_strategy; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.library_strategy AS ENUM (
    'WGS',
    'WXS',
    'RNA-Seq',
    'ChIP-Seq',
    'miRNA-Seq',
    'Bisulfite-Seq',
    'Validation',
    'Amplicon',
    'Other'
);


ALTER TYPE public.library_strategy OWNER TO postgres;

--
-- Name: sample_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.sample_type AS ENUM (
    'DNA',
    'FFPE DNA',
    'Amplified DNA',
    'RNA',
    'Total RNA',
    'FFPE RNA'
);


ALTER TYPE public.sample_type OWNER TO postgres;

--
-- Name: specimen_class; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.specimen_class AS ENUM (
    'Normal',
    'Tumour',
    'Adjacent normal'
);


ALTER TYPE public.specimen_class OWNER TO postgres;

--
-- Name: specimen_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.specimen_type AS ENUM (
    'Normal - solid tissue',
    'Normal - blood derived',
    'Normal - bone marrow',
    'Normal - tissue adjacent to primary',
    'Normal - buccal cell',
    'Normal - EBV immortalized',
    'Normal - lymph node',
    'Normal - other',
    'Primary tumour',
    'Primary tumour - solid tissue',
    'Primary tumour - blood derived (peripheral blood)',
    'Primary tumour - blood derived (bone marrow)',
    'Primary tumour - additional new primary',
    'Primary tumour - other',
    'Recurrent tumour - solid tissue',
    'Recurrent tumour - blood derived (peripheral blood)',
    'Recurrent tumour - blood derived (bone marrow)',
    'Recurrent tumour - other',
    'Metastatic tumour - NOS',
    'Metastatic tumour - lymph node',
    'Metastatic tumour - metastasis local to lymph node',
    'Metastatic tumour - metastasis to distant location',
    'Metastatic tumour - additional metastatic',
    'Xenograft - derived from primary tumour',
    'Xenograft - derived from tumour cell line',
    'Cell line - derived from tumour',
    'Primary tumour - lymph node',
    'Metastatic tumour - other',
    'Cell line - derived from xenograft tumour'
);


ALTER TYPE public.specimen_type OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: analysis; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis (
    id character varying(36) NOT NULL,
    study_id character varying(36),
    type public.analysis_type,
    state public.analysis_state,
    analysis_schema_id integer,
    analysis_data_id integer
);


ALTER TABLE public.analysis OWNER TO postgres;

--
-- Name: analysis_data; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_data (
    id bigint NOT NULL,
    data jsonb NOT NULL
);


ALTER TABLE public.analysis_data OWNER TO postgres;

--
-- Name: analysis_data_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.analysis_data_id_seq OWNER TO postgres;

--
-- Name: analysis_data_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.analysis_data_id_seq OWNED BY public.analysis_data.id;


--
-- Name: analysis_schema; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_schema (
    id bigint NOT NULL,
    version integer,
    name character varying(225) NOT NULL,
    schema jsonb NOT NULL
);


ALTER TABLE public.analysis_schema OWNER TO postgres;

--
-- Name: analysis_schema_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_schema_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.analysis_schema_id_seq OWNER TO postgres;

--
-- Name: analysis_schema_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.analysis_schema_id_seq OWNED BY public.analysis_schema.id;


--
-- Name: donor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.donor (
    id character varying(36) NOT NULL,
    study_id character varying(36),
    submitter_id text,
    gender public.gender
);


ALTER TABLE public.donor OWNER TO postgres;

--
-- Name: sample; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sample (
    id character varying(36) NOT NULL,
    specimen_id character varying(36),
    submitter_id text,
    type public.sample_type
);


ALTER TABLE public.sample OWNER TO postgres;

--
-- Name: specimen; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.specimen (
    id character varying(36) NOT NULL,
    donor_id character varying(36),
    submitter_id text,
    class public.specimen_class,
    type public.specimen_type
);


ALTER TABLE public.specimen OWNER TO postgres;

--
-- Name: study; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.study (
    id character varying(36) NOT NULL,
    name text,
    description text,
    organization text
);


ALTER TABLE public.study OWNER TO postgres;

--
-- Name: businesskeyview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.businesskeyview AS
 SELECT s.id AS study_id,
    sp.id AS specimen_id,
    sp.submitter_id AS specimen_submitter_id,
    sa.id AS sample_id,
    sa.submitter_id AS sample_submitter_id
   FROM (((public.study s
     JOIN public.donor d ON (((s.id)::text = (d.study_id)::text)))
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)));


ALTER TABLE public.businesskeyview OWNER TO postgres;

--
-- Name: file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file (
    id character varying(36) NOT NULL,
    analysis_id character varying(36),
    study_id character varying(36),
    name text,
    size bigint,
    md5 character(32),
    type public.file_type,
    access public.access_type
);


ALTER TABLE public.file OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: sampleset; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sampleset (
    analysis_id character varying(36),
    sample_id character varying(36)
);


ALTER TABLE public.sampleset OWNER TO postgres;

--
-- Name: idview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.idview AS
 SELECT DISTINCT a.id AS analysis_id,
    ans.id AS analysis_schema_id,
    ans.name AS analysis_schema_name,
    a.state AS analysis_state,
    a.study_id,
    d.id AS donor_id,
    sp.id AS specimen_id,
    sa.id AS sample_id,
    f.id AS object_id
   FROM ((((((public.donor d
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)))
     JOIN public.sampleset sas ON (((sa.id)::text = (sas.sample_id)::text)))
     JOIN public.file f ON (((sas.analysis_id)::text = (f.analysis_id)::text)))
     JOIN public.analysis a ON (((sas.analysis_id)::text = (a.id)::text)))
     JOIN public.analysis_schema ans ON ((a.analysis_schema_id = ans.id)));


ALTER TABLE public.idview OWNER TO postgres;

--
-- Name: info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.info (
    id character varying(36),
    id_type public.id_type,
    info json
);


ALTER TABLE public.info OWNER TO postgres;

--
-- Name: infoview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.infoview AS
 SELECT a.id AS analysis_id,
    i_study.info AS study_info,
    i_donor.info AS donor_info,
    i_sp.info AS specimen_info,
    i_sa.info AS sample_info,
    i_a.info AS analysis_info,
    i_f.info AS file_info
   FROM ((((((((((((public.study s
     JOIN public.info i_study ON ((((i_study.id)::text = (s.id)::text) AND (i_study.id_type = 'Study'::public.id_type))))
     JOIN public.donor d ON (((s.id)::text = (d.study_id)::text)))
     JOIN public.info i_donor ON ((((i_donor.id)::text = (d.id)::text) AND (i_donor.id_type = 'Donor'::public.id_type))))
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.info i_sp ON ((((i_sp.id)::text = (sp.id)::text) AND (i_sp.id_type = 'Specimen'::public.id_type))))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)))
     JOIN public.info i_sa ON ((((i_sa.id)::text = (sa.id)::text) AND (i_sa.id_type = 'Sample'::public.id_type))))
     JOIN public.sampleset ss ON (((sa.id)::text = (ss.sample_id)::text)))
     JOIN public.analysis a ON (((ss.analysis_id)::text = (a.id)::text)))
     JOIN public.info i_a ON ((((i_a.id)::text = (a.id)::text) AND (i_a.id_type = 'Analysis'::public.id_type))))
     JOIN public.file f ON (((a.id)::text = (f.analysis_id)::text)))
     JOIN public.info i_f ON ((((i_f.id)::text = (f.id)::text) AND (i_f.id_type = 'File'::public.id_type))));


ALTER TABLE public.infoview OWNER TO postgres;

--
-- Name: upload; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.upload (
    id character varying(40) NOT NULL,
    study_id character varying(36),
    analysis_id text,
    state character varying(50),
    errors text,
    payload text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.upload OWNER TO postgres;

--
-- Name: analysis_data id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_data ALTER COLUMN id SET DEFAULT nextval('public.analysis_data_id_seq'::regclass);


--
-- Name: analysis_schema id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_schema ALTER COLUMN id SET DEFAULT nextval('public.analysis_schema_id_seq'::regclass);


--
-- Data for Name: analysis; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis (id, study_id, type, state, analysis_schema_id, analysis_data_id) FROM stdin;
735b65fa-f502-11e9-9811-6d6ef1d32823	ABC123	\N	UNPUBLISHED	1	1
\.


--
-- Data for Name: analysis_data; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_data (id, data) FROM stdin;
1	{"info": {"randomField19": "alternatively, put some extra ANALYSIS fields here"}, "experiment": {"info": {"randomField16": "alternatively, put some extra EXPERIMENT fields here"}, "randomField14": "we can define any EXPERIMENT field. For example, randomField14", "randomField15": "as a second example, we can define another random EXPERIMENT field called randomField15", "variantCallingTool": "silver bullet", "matchedNormalSampleSubmitterId": "sample x24-11a"}, "randomField17": "we can define any ANALYSIS field. For example, randomField17", "randomField18": "as a second example, we can define another random ANALYSIS field called randomField18"}
\.


--
-- Name: analysis_data_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.analysis_data_id_seq', 1, true);


--
-- Data for Name: analysis_schema; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_schema (id, version, name, schema) FROM stdin;
1	1	variantCall	{"type": "object", "required": ["experiment"], "properties": {"experiment": {"type": "object", "required": ["matchedNormalSampleSubmitterId", "variantCallingTool"], "properties": {"variantCallingTool": {"type": "string"}, "matchedNormalSampleSubmitterId": {"type": "string"}}}}}
2	1	sequencingRead	{"type": "object", "required": ["experiment"], "properties": {"experiment": {"type": "object", "required": ["libraryStrategy"], "properties": {"aligned": {"type": ["boolean", "null"]}, "pairedEnd": {"type": ["boolean", "null"]}, "insertSize": {"type": ["integer", "null"]}, "alignmentTool": {"type": ["string", "null"]}, "libraryStrategy": {"enum": ["WGS", "WXS", "RNA-Seq", "ChIP-Seq", "miRNA-Seq", "Bisulfite-Seq", "Validation", "Amplicon", "Other"], "type": "string"}, "referenceGenome": {"type": ["string", "null"]}}}}}
\.


--
-- Name: analysis_schema_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.analysis_schema_id_seq', 2, true);


--
-- Data for Name: donor; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.donor (id, study_id, submitter_id, gender) FROM stdin;
DO6cbf73d97b258bcaab5263fa193cb53b	ABC123	internal_donor_123456789-00	female
\.


--
-- Data for Name: file; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file (id, analysis_id, study_id, name, size, md5, type, access) FROM stdin;
5be58fbb-775b-5259-bbbd-555e07fbdf24	735b65fa-f502-11e9-9811-6d6ef1d32823	ABC123	example.vcf.gz	52	9a793e90d0d1e11301ea8da996446e59	VCF	open
632c29af-c46e-581b-ab43-65e875d86361	735b65fa-f502-11e9-9811-6d6ef1d32823	ABC123	example.vcf.gz.idx	25	c03274816eb4907a92b8e5632cd6eb81	IDX	open
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	Base version	SQL	V1__Base_version.sql	-1608472095	postgres	2019-10-22 19:30:10.105505	493	t
2	1.1	added schema	SQL	V1_1__added_schema.sql	675033696	postgres	2019-10-22 19:30:10.625976	30	t
3	1.2	dynamic schema integration	SPRING_JDBC	db.migration.V1_2__dynamic_schema_integration	\N	postgres	2019-10-22 19:30:10.679764	141	t
4	1.3	post schema integration	SQL	V1_3__post_schema_integration.sql	1429883245	postgres	2019-10-22 19:30:10.885393	13	t
\.


--
-- Data for Name: info; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.info (id, id_type, info) FROM stdin;
ABC123	Study	{}
DO6cbf73d97b258bcaab5263fa193cb53b	Donor	{"randomField4":"we can define any DONOR field. For example, randomField4","randomField5":"as a second example, we can define another random DONOR field called randomField5","randomField6":"alternatively, put some extra DONOR fields here"}
SP5cabc533f4329c31f2b6adabbf1c9800	Specimen	{"randomField1":"we can define any SPECIMEN field. For example, randomField1","randomField2":"as a second example, we can define another random SPECIMEN field called randomField2","randomField3":"alternatively, put some extra SPECIMEN fields here"}
SA17e38fb4c5969ce8e34c9c209650d50b	Sample	{"randomField7":"we can define any SAMPLE field. For example, randomField7","randomField8":"as a second example, we can define another random SAMPLE field called randomField8","randomField9":"alternatively, put some extra SAMPLE fields here"}
5be58fbb-775b-5259-bbbd-555e07fbdf24	File	{"randomField10":"we can define any FILE field. For example, randomField10","randomField11":"as a second example, we can define another random FILE field called randomField11","randomField12":"alternatively, put some extra FILE fields here"}
632c29af-c46e-581b-ab43-65e875d86361	File	{"randomField10":"we can define any FILE field. For example, randomField10","randomField12":"alternatively, put some extra FILE fields here","randomField13":"as a second example, we can define another random FILE field called randomField13"}
\.


--
-- Data for Name: sample; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sample (id, specimen_id, submitter_id, type) FROM stdin;
SA17e38fb4c5969ce8e34c9c209650d50b	SP5cabc533f4329c31f2b6adabbf1c9800	internal_sample_98024759826836	Total RNA
\.


--
-- Data for Name: sampleset; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sampleset (analysis_id, sample_id) FROM stdin;
735b65fa-f502-11e9-9811-6d6ef1d32823	SA17e38fb4c5969ce8e34c9c209650d50b
\.


--
-- Data for Name: specimen; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.specimen (id, donor_id, submitter_id, class, type) FROM stdin;
SP5cabc533f4329c31f2b6adabbf1c9800	DO6cbf73d97b258bcaab5263fa193cb53b	internal_specimen_9b73gk8s02dk	Tumour	Primary tumour - other
\.


--
-- Data for Name: study; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.study (id, name, description, organization) FROM stdin;
ABC123	\N	\N	\N
\.


--
-- Data for Name: upload; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.upload (id, study_id, analysis_id, state, errors, payload, created_at, updated_at) FROM stdin;
\.


--
-- Name: analysis_data analysis_data_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_data
    ADD CONSTRAINT analysis_data_pkey PRIMARY KEY (id);


--
-- Name: analysis analysis_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_pkey PRIMARY KEY (id);


--
-- Name: analysis_schema analysis_schema_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_schema
    ADD CONSTRAINT analysis_schema_pkey PRIMARY KEY (id);


--
-- Name: donor donor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.donor
    ADD CONSTRAINT donor_pkey PRIMARY KEY (id);


--
-- Name: file file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: sample sample_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_pkey PRIMARY KEY (id);


--
-- Name: specimen specimen_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.specimen
    ADD CONSTRAINT specimen_pkey PRIMARY KEY (id);


--
-- Name: study study_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study
    ADD CONSTRAINT study_pkey PRIMARY KEY (id);


--
-- Name: upload upload_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.upload
    ADD CONSTRAINT upload_pkey PRIMARY KEY (id);


--
-- Name: analysis_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX analysis_id_study_id_uindex ON public.analysis USING btree (id, study_id);


--
-- Name: analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX analysis_id_uindex ON public.analysis USING btree (id);


--
-- Name: analysis_schema_name_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_name_index ON public.analysis_schema USING btree (name);


--
-- Name: analysis_schema_name_version_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_name_version_index ON public.analysis_schema USING btree (name, version);


--
-- Name: analysis_schema_version_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_version_index ON public.analysis_schema USING btree (version);


--
-- Name: analysis_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_study_id_uindex ON public.analysis USING btree (study_id);


--
-- Name: donor_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_id_study_id_uindex ON public.donor USING btree (id, study_id);


--
-- Name: donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_id_uindex ON public.donor USING btree (id);


--
-- Name: donor_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX donor_study_id_uindex ON public.donor USING btree (study_id);


--
-- Name: donor_submitter_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_submitter_id_study_id_uindex ON public.donor USING btree (submitter_id, study_id);


--
-- Name: donor_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX donor_submitter_id_uindex ON public.donor USING btree (submitter_id);


--
-- Name: file_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_analysis_id_uindex ON public.file USING btree (analysis_id);


--
-- Name: file_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX file_id_analysis_id_uindex ON public.file USING btree (id, analysis_id);


--
-- Name: file_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX file_id_index ON public.file USING btree (id);


--
-- Name: file_name_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_name_analysis_id_uindex ON public.file USING btree (name, analysis_id);


--
-- Name: file_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_study_id_uindex ON public.file USING btree (study_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: info_id_id_type_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX info_id_id_type_uindex ON public.info USING btree (id, id_type);


--
-- Name: info_id_type_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX info_id_type_uindex ON public.info USING btree (id_type);


--
-- Name: info_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX info_id_uindex ON public.info USING btree (id);


--
-- Name: sample_id_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_id_specimen_id_uindex ON public.sample USING btree (id, specimen_id);


--
-- Name: sample_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_id_uindex ON public.sample USING btree (id);


--
-- Name: sample_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sample_specimen_id_uindex ON public.sample USING btree (specimen_id);


--
-- Name: sample_submitter_id_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_submitter_id_specimen_id_uindex ON public.sample USING btree (submitter_id, specimen_id);


--
-- Name: sample_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sample_submitter_id_uindex ON public.sample USING btree (submitter_id);


--
-- Name: sampleset_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_analysis_id_uindex ON public.sampleset USING btree (analysis_id);


--
-- Name: sampleset_sample_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_sample_id_analysis_id_uindex ON public.sampleset USING btree (sample_id, analysis_id);


--
-- Name: sampleset_sample_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_sample_id_uindex ON public.sampleset USING btree (sample_id);


--
-- Name: specimen_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX specimen_donor_id_uindex ON public.specimen USING btree (donor_id);


--
-- Name: specimen_id_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_id_donor_id_uindex ON public.specimen USING btree (id, donor_id);


--
-- Name: specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_id_uindex ON public.specimen USING btree (id);


--
-- Name: specimen_submitter_id_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_submitter_id_donor_id_uindex ON public.specimen USING btree (submitter_id, donor_id);


--
-- Name: specimen_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX specimen_submitter_id_uindex ON public.specimen USING btree (submitter_id);


--
-- Name: study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX study_id_uindex ON public.study USING btree (id);


--
-- Name: upload_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX upload_id_uindex ON public.upload USING btree (id);


--
-- Name: upload_study_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX upload_study_id_analysis_id_uindex ON public.upload USING btree (study_id, analysis_id);


--
-- Name: analysis analysis_data_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_data_id_fk FOREIGN KEY (analysis_data_id) REFERENCES public.analysis_data(id);


--
-- Name: analysis analysis_schema_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_schema_id_fk FOREIGN KEY (analysis_schema_id) REFERENCES public.analysis_schema(id);


--
-- Name: analysis analysis_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: donor donor_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.donor
    ADD CONSTRAINT donor_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: file file_analysis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: file file_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: sample sample_specimen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_specimen_id_fkey FOREIGN KEY (specimen_id) REFERENCES public.specimen(id);


--
-- Name: sampleset sampleset_analysis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sampleset
    ADD CONSTRAINT sampleset_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: sampleset sampleset_sample_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sampleset
    ADD CONSTRAINT sampleset_sample_id_fkey FOREIGN KEY (sample_id) REFERENCES public.sample(id);


--
-- Name: specimen specimen_donor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.specimen
    ADD CONSTRAINT specimen_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES public.donor(id);


--
-- Name: upload upload_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.upload
    ADD CONSTRAINT upload_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- PostgreSQL database dump complete
--

