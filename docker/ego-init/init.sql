--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.16
-- Dumped by pg_dump version 9.5.16

SET statement_timeout = 0;
SET lock_timeout = 0;
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
-- Name: aclmask; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.aclmask AS ENUM (
    'READ',
    'WRITE',
    'DENY'
);


ALTER TYPE public.aclmask OWNER TO postgres;

--
-- Name: applicationtype; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.applicationtype AS ENUM (
    'CLIENT',
    'ADMIN'
);


ALTER TYPE public.applicationtype OWNER TO postgres;

--
-- Name: languagetype; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.languagetype AS ENUM (
    'ENGLISH',
    'FRENCH',
    'SPANISH'
);


ALTER TYPE public.languagetype OWNER TO postgres;

--
-- Name: statustype; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.statustype AS ENUM (
    'APPROVED',
    'REJECTED',
    'DISABLED',
    'PENDING'
);


ALTER TYPE public.statustype OWNER TO postgres;

--
-- Name: usertype; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.usertype AS ENUM (
    'USER',
    'ADMIN'
);


ALTER TYPE public.usertype OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: egoapplication; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.egoapplication (
    name character varying(255) NOT NULL,
    clientid character varying(255) NOT NULL,
    clientsecret character varying(255) NOT NULL,
    redirecturi text,
    description text,
    status public.statustype DEFAULT 'PENDING'::public.statustype NOT NULL,
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    type public.applicationtype DEFAULT 'CLIENT'::public.applicationtype NOT NULL
);


ALTER TABLE public.egoapplication OWNER TO postgres;

--
-- Name: egogroup; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.egogroup (
    name character varying(255) NOT NULL,
    description character varying(255),
    status public.statustype DEFAULT 'PENDING'::public.statustype NOT NULL,
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL
);


ALTER TABLE public.egogroup OWNER TO postgres;

--
-- Name: egouser; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.egouser (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    type public.usertype DEFAULT 'USER'::public.usertype NOT NULL,
    firstname text DEFAULT ''::text NOT NULL,
    lastname text DEFAULT ''::text NOT NULL,
    createdat timestamp without time zone NOT NULL,
    lastlogin timestamp without time zone,
    status public.statustype DEFAULT 'PENDING'::public.statustype NOT NULL,
    preferredlanguage public.languagetype
);


ALTER TABLE public.egouser OWNER TO postgres;

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
-- Name: groupapplication; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.groupapplication (
    group_id uuid NOT NULL,
    application_id uuid NOT NULL
);


ALTER TABLE public.groupapplication OWNER TO postgres;

--
-- Name: grouppermission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.grouppermission (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    policy_id uuid,
    group_id uuid,
    access_level public.aclmask NOT NULL
);


ALTER TABLE public.grouppermission OWNER TO postgres;

--
-- Name: policy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.policy (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    owner uuid,
    name character varying(255) NOT NULL
);


ALTER TABLE public.policy OWNER TO postgres;

--
-- Name: token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.token (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(2048) NOT NULL,
    owner uuid NOT NULL,
    issuedate timestamp without time zone DEFAULT now() NOT NULL,
    isrevoked boolean DEFAULT false NOT NULL,
    description character varying(255),
    expirydate timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.token OWNER TO postgres;

--
-- Name: tokenscope; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tokenscope (
    token_id uuid NOT NULL,
    policy_id uuid NOT NULL,
    access_level public.aclmask NOT NULL
);


ALTER TABLE public.tokenscope OWNER TO postgres;

--
-- Name: userapplication; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.userapplication (
    user_id uuid NOT NULL,
    application_id uuid NOT NULL
);


ALTER TABLE public.userapplication OWNER TO postgres;

--
-- Name: usergroup; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usergroup (
    user_id uuid NOT NULL,
    group_id uuid NOT NULL
);


ALTER TABLE public.usergroup OWNER TO postgres;

--
-- Name: userpermission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.userpermission (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    policy_id uuid,
    user_id uuid,
    access_level public.aclmask NOT NULL
);


ALTER TABLE public.userpermission OWNER TO postgres;

--
-- Data for Name: egoapplication; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.egoapplication (name, clientid, clientsecret, redirecturi, description, status, id, type) FROM stdin;
\.


--
-- Data for Name: egogroup; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.egogroup (name, description, status, id) FROM stdin;
overture-admin	Admin	APPROVED	f2885e96-f74e-4f7a-b935-fb48b18e761d
\.


--
-- Data for Name: egouser; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.egouser (id, name, email, type, firstname, lastname, createdat, lastlogin, status, preferredlanguage) FROM stdin;
c6608c3e-1181-4957-99c4-094493391096	john.doe@example.com	john.doe@example.com	USER	John	Doe	2019-11-20 20:38:33.815	\N	APPROVED	ENGLISH
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	initial database	SQL	V1__initial_database.sql	-556387533	postgres	2019-11-20 20:35:19.523848	54	t
2	1.1	complete uuid migration	SPRING_JDBC	db.migration.V1_1__complete_uuid_migration	\N	postgres	2019-11-20 20:35:19.591892	75	t
3	1.2	acl expansion	SQL	V1_2__acl_expansion.sql	-125082215	postgres	2019-11-20 20:35:19.675498	19	t
4	1.3	string to date	SPRING_JDBC	db.migration.V1_3__string_to_date	\N	postgres	2019-11-20 20:35:19.701898	28	t
5	1.4	score integration	SQL	V1_4__score_integration.sql	323452398	postgres	2019-11-20 20:35:19.738191	13	t
6	1.5	table renaming	SQL	V1_5__table_renaming.sql	480984865	postgres	2019-11-20 20:35:19.758578	11	t
7	1.6	add not null constraint	SQL	V1_6__add_not_null_constraint.sql	1562044084	postgres	2019-11-20 20:35:19.776433	7	t
8	1.7	token modification	SQL	V1_7__token_modification.sql	-11736908	postgres	2019-11-20 20:35:19.789578	5	t
9	1.8	application types	SQL	V1_8__application_types.sql	-1894533468	postgres	2019-11-20 20:35:19.801421	13	t
10	1.9	new enum types	SQL	V1_9__new_enum_types.sql	1135272560	postgres	2019-11-20 20:35:19.821124	84	t
11	1.10	remove apps from apitokens	SQL	V1_10__remove_apps_from_apitokens.sql	-1412739333	postgres	2019-11-20 20:35:19.915229	2	t
12	1.11	add expiry date api tokens	SQL	V1_11__add_expiry_date_api_tokens.sql	-774407414	postgres	2019-11-20 20:35:19.923177	11	t
13	1.12	egoapplication unique constraints	SQL	V1_12__egoapplication_unique_constraints.sql	1415229200	postgres	2019-11-20 20:35:19.940859	4	t
14	1.13	fname lname not null constraints	SQL	V1_13__fname_lname_not_null_constraints.sql	148150980	postgres	2019-11-20 20:35:19.950402	3	t
15	1.14	indices	SQL	V1_14__indices.sql	1170056158	postgres	2019-11-20 20:35:19.959073	40	t
\.


--
-- Data for Name: groupapplication; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.groupapplication (group_id, application_id) FROM stdin;
\.


--
-- Data for Name: grouppermission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.grouppermission (id, policy_id, group_id, access_level) FROM stdin;
9e361c69-7d3e-4144-a638-0ebda207b50a	4b7718ce-ad94-4ec5-b0fb-bf91a520a816	f2885e96-f74e-4f7a-b935-fb48b18e761d	WRITE
f781e10a-f4b8-4dd1-a9c6-75a9193d91ff	7978c66c-7bd6-4d7b-a6e2-418ab6714859	f2885e96-f74e-4f7a-b935-fb48b18e761d	WRITE
\.


--
-- Data for Name: policy; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.policy (id, owner, name) FROM stdin;
4b7718ce-ad94-4ec5-b0fb-bf91a520a816	\N	song
7978c66c-7bd6-4d7b-a6e2-418ab6714859	\N	score
ed5149c4-d8c3-46f8-ab01-b903f82b5fe3	\N	score.TEST-CA
\.


--
-- Data for Name: token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.token (id, name, owner, issuedate, isrevoked, description, expirydate) FROM stdin;
f7d708ef-41f8-493f-ad8e-cb0ac97b0688	f69b726d-d40f-4261-b105-1ec7e6bf04d5	c6608c3e-1181-4957-99c4-094493391096	2023-11-02 20:52:08.247	f	\N	2030-11-02 20:52:08.247
fafaac34-6b01-47ef-9ae6-6d8cb30af5ca	fd0c6c40-254b-4a5f-82e7-cf21a380ccb3	c6608c3e-1181-4957-99c4-094493391096	2023-11-02 20:55:56.186	f	\N	2030-11-02 20:55:56.186
7df26ca6-801f-4302-a318-6f766d759b1d	1f070fb0-0ee4-4815-8097-b5b065c661cc	c6608c3e-1181-4957-99c4-094493391096	2023-11-02 20:57:38.345	f	\N	2030-11-02 20:57:38.345
\.


--
-- Data for Name: tokenscope; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tokenscope (token_id, policy_id, access_level) FROM stdin;
f7d708ef-41f8-493f-ad8e-cb0ac97b0688	4b7718ce-ad94-4ec5-b0fb-bf91a520a816	WRITE
f7d708ef-41f8-493f-ad8e-cb0ac97b0688	7978c66c-7bd6-4d7b-a6e2-418ab6714859	WRITE
fafaac34-6b01-47ef-9ae6-6d8cb30af5ca	ed5149c4-d8c3-46f8-ab01-b903f82b5fe3	WRITE
7df26ca6-801f-4302-a318-6f766d759b1d	ed5149c4-d8c3-46f8-ab01-b903f82b5fe3	WRITE
7df26ca6-801f-4302-a318-6f766d759b1d	4b7718ce-ad94-4ec5-b0fb-bf91a520a816	WRITE
\.


--
-- Data for Name: userapplication; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.userapplication (user_id, application_id) FROM stdin;
\.


--
-- Data for Name: usergroup; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usergroup (user_id, group_id) FROM stdin;
c6608c3e-1181-4957-99c4-094493391096	f2885e96-f74e-4f7a-b935-fb48b18e761d
\.


--
-- Data for Name: userpermission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.userpermission (id, policy_id, user_id, access_level) FROM stdin;
a66e447c-0f11-45b4-aaa0-a878b911d688	ed5149c4-d8c3-46f8-ab01-b903f82b5fe3	c6608c3e-1181-4957-99c4-094493391096	WRITE
\.


--
-- Name: egoapplication_clientid_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egoapplication
    ADD CONSTRAINT egoapplication_clientid_key UNIQUE (clientid);


--
-- Name: egoapplication_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egoapplication
    ADD CONSTRAINT egoapplication_name_key UNIQUE (name);


--
-- Name: egoapplication_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egoapplication
    ADD CONSTRAINT egoapplication_pkey PRIMARY KEY (id);


--
-- Name: egogroup_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egogroup
    ADD CONSTRAINT egogroup_name_key UNIQUE (name);


--
-- Name: egogroup_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egogroup
    ADD CONSTRAINT egogroup_pkey PRIMARY KEY (id);


--
-- Name: egouser_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egouser
    ADD CONSTRAINT egouser_email_key UNIQUE (email);


--
-- Name: egouser_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egouser
    ADD CONSTRAINT egouser_name_key UNIQUE (name);


--
-- Name: egouser_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.egouser
    ADD CONSTRAINT egouser_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: grouppermission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.grouppermission
    ADD CONSTRAINT grouppermission_pkey PRIMARY KEY (id);


--
-- Name: policy_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.policy
    ADD CONSTRAINT policy_name_key UNIQUE (name);


--
-- Name: policy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.policy
    ADD CONSTRAINT policy_pkey PRIMARY KEY (id);


--
-- Name: token_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.token
    ADD CONSTRAINT token_name_key UNIQUE (name);


--
-- Name: token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.token
    ADD CONSTRAINT token_pkey PRIMARY KEY (id);


--
-- Name: userpermission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.userpermission
    ADD CONSTRAINT userpermission_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_grouppermission_both; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_grouppermission_both ON public.grouppermission USING btree (group_id, policy_id);


--
-- Name: idx_grouppermission_group; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_grouppermission_group ON public.grouppermission USING btree (group_id);


--
-- Name: idx_grouppermission_policy; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_grouppermission_policy ON public.grouppermission USING btree (policy_id);


--
-- Name: idx_token_owner; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_token_owner ON public.token USING btree (owner);


--
-- Name: idx_tokenscope; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tokenscope ON public.tokenscope USING btree (token_id, policy_id, access_level);


--
-- Name: idx_tokenscope_policy; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tokenscope_policy ON public.tokenscope USING btree (policy_id);


--
-- Name: idx_usergroup_both; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_usergroup_both ON public.usergroup USING btree (user_id, group_id);


--
-- Name: idx_usergroup_group; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_usergroup_group ON public.usergroup USING btree (group_id);


--
-- Name: idx_usergroup_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_usergroup_user ON public.usergroup USING btree (user_id);


--
-- Name: idx_userpermission_both; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_userpermission_both ON public.userpermission USING btree (user_id, policy_id);


--
-- Name: idx_userpermission_policy; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_userpermission_policy ON public.userpermission USING btree (policy_id);


--
-- Name: idx_userpermission_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_userpermission_user ON public.userpermission USING btree (user_id);


--
-- Name: groupapplication_application_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groupapplication
    ADD CONSTRAINT groupapplication_application_fkey FOREIGN KEY (application_id) REFERENCES public.egoapplication(id);


--
-- Name: groupapplication_group_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groupapplication
    ADD CONSTRAINT groupapplication_group_fkey FOREIGN KEY (group_id) REFERENCES public.egogroup(id);


--
-- Name: grouppermission_group_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.grouppermission
    ADD CONSTRAINT grouppermission_group_fkey FOREIGN KEY (group_id) REFERENCES public.egogroup(id);


--
-- Name: grouppermission_policy_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.grouppermission
    ADD CONSTRAINT grouppermission_policy_fkey FOREIGN KEY (policy_id) REFERENCES public.policy(id);


--
-- Name: policy_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.policy
    ADD CONSTRAINT policy_owner_fkey FOREIGN KEY (owner) REFERENCES public.egogroup(id);


--
-- Name: token_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.token
    ADD CONSTRAINT token_owner_fkey FOREIGN KEY (owner) REFERENCES public.egouser(id);


--
-- Name: tokenscope_policy_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tokenscope
    ADD CONSTRAINT tokenscope_policy_fkey FOREIGN KEY (policy_id) REFERENCES public.policy(id);


--
-- Name: tokenscope_token_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tokenscope
    ADD CONSTRAINT tokenscope_token_fkey FOREIGN KEY (token_id) REFERENCES public.token(id);


--
-- Name: userapplication_application_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.userapplication
    ADD CONSTRAINT userapplication_application_fkey FOREIGN KEY (application_id) REFERENCES public.egoapplication(id);


--
-- Name: userapplication_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.userapplication
    ADD CONSTRAINT userapplication_user_fkey FOREIGN KEY (user_id) REFERENCES public.egouser(id);


--
-- Name: usergroup_group_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usergroup
    ADD CONSTRAINT usergroup_group_fkey FOREIGN KEY (group_id) REFERENCES public.egogroup(id);


--
-- Name: usergroup_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usergroup
    ADD CONSTRAINT usergroup_user_fkey FOREIGN KEY (user_id) REFERENCES public.egouser(id);


--
-- Name: userpermission_policy_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.userpermission
    ADD CONSTRAINT userpermission_policy_fkey FOREIGN KEY (policy_id) REFERENCES public.policy(id);


--
-- Name: userpermission_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.userpermission
    ADD CONSTRAINT userpermission_user_fkey FOREIGN KEY (user_id) REFERENCES public.egouser(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

