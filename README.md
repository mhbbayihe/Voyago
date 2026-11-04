# VoyaGo : plateforme de réservation de billets de voyage

Projet du **Groupe 15** pour la matière *Architecture logicielle et objets répartis* (ENSP Douala, GIT Génie Logiciel, niveau 5, 2026-2027, M. Elve Charlu Tchuigoua).

VoyaGo est une application web **Spring Boot**. Elle permet de rechercher un voyage en **bus, train ou avion** au Cameroun, de choisir son siège sur un plan, de payer par **MTN MoMo ou Orange Money** (simulés) et de recevoir un **billet PDF avec QR code signé**. Ce billet est ensuite contrôlé à l'embarquement.

---

## 1. Lancer l'application

**Prérequis :** Java 17 ou plus récent. Rien d'autre n'est à installer : la base de données H2 est intégrée et Maven est fourni par le script `mvnw`.

```bash
# Windows (PowerShell ou invite de commandes)
mvnw.cmd spring-boot:run

# Linux / macOS / Git Bash
./mvnw spring-boot:run
```

Ouvrez ensuite **http://localhost:8080**.

Au premier démarrage, l'application crée un jeu de démonstration, ce qui prend quelques secondes :
- 5 compagnies fictives (3 de bus, 1 de train, 1 aérienne) ;
- 15 gares et aéroports et 32 lignes ;
- environ 1 100 voyages répartis sur 10 jours ;
- des réservations déjà payées.

Pour repartir de zéro, arrêtez l'application et supprimez le dossier `data/`.

### Comptes de démonstration

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Client | `client@voyago.cm` | `Client@2026` |
| Agent bus (Sanaga Express) | `agent@sanaga.cm` | `Agent@2026` |
| Agent bus (Wouri Voyages) | `agent@wouri.cm` | `Agent@2026` |
| Agent train (Rail Cameroun Intercity) | `agent@rail.cm` | `Agent@2026` |
| Agent avion (Air Mungo) | `agent@airmungo.cm` | `Agent@2026` |
| Administrateur | `admin@voyago.cm` | `Admin@2026` |

La page de connexion propose ces comptes : un clic suffit pour remplir les champs.

### Adresses utiles

| URL | Contenu |
|---|---|
| http://localhost:8080 | Site web (client, agent, administrateur) |
| http://localhost:8080/swagger-ui.html | Documentation interactive de l'API REST (OpenAPI 3) |
| http://localhost:8080/about | Résumé de l'architecture |
| http://localhost:8080/h2-console | Console de la base (administrateur ; JDBC `jdbc:h2:file:./data/voyago`, utilisateur `sa`, sans mot de passe) |
| http://localhost:8080/actuator/health | État de santé |

---

## 2. Scénario de démonstration (cahier des charges, § 16.2)

1. **Recherche responsive.** Sur un téléphone (ou avec l'outil « mode appareil » du navigateur), recherchez Douala → Yaoundé pour demain. Les résultats proposent le bus, le train et l'avion, avec des filtres et un tri.
2. **Verrou de siège.** Connecté en tant que `client@voyago.cm`, choisissez deux sièges et saisissez les passagers. Dans un **second navigateur** (ou une fenêtre de navigation privée), connectez-vous avec un autre compte et essayez de prendre les mêmes sièges. Ils apparaissent comme occupés ; si les deux clients valident en même temps, le second est refusé.
3. **Paiement Mobile Money (simulé).** Choisissez MTN MoMo et saisissez un numéro en `677…`. Sur l'écran « Validez sur votre téléphone », cliquez sur **Valider**. Les billets avec QR code s'affichent, et l'e-mail avec le PDF en pièce jointe est visible dans **Messages**.
4. **Contrôle à l'embarquement.** Connecté en tant que `agent@sanaga.cm`, ouvrez **Contrôle des billets** et saisissez le numéro du billet `TK-…` (ou scannez le QR code avec la caméra). Le premier contrôle est accepté (vert). Le **second est refusé** (rouge, « déjà utilisé »).
5. **Annulation.** Le client annule une autre réservation et voit le montant remboursé selon le barème : 80 % à plus de 24 h du départ, 50 % entre 24 h et 2 h, 0 % à moins de 2 h.
6. **Statistiques.** En tant qu'administrateur, consultez le tableau de bord (revenus, billets, remplissage, top des lignes, répartition par mode), les paiements et remboursements, et le journal d'audit.

Autres fonctions à montrer :
- la **vente au guichet**, avec encaissement en espèces (agent → Voyages → Vendre) ;
- la **programmation de voyages** et la modification d'un tarif, tracée dans l'audit ;
- la liste des passagers d'un voyage ;
- la gestion des compagnies, des gares, des lignes et des comptes (administrateur).

---

## 3. Architecture

### Style retenu : monolithe modulaire, hexagonal dans chaque module

C'est le **plan de repli prévu au cahier des charges (§ 6.2)**, adapté au délai. L'application est découpée en modules indépendants :

```
cm.enspd.voyago
├── auth/          comptes, rôles, JWT
├── trip/          compagnies, gares, lignes, véhicules, voyages, recherche
├── booking/       réservations, verrou des sièges, billets, QR, PDF
├── payment/       paiements Mobile Money, webhooks, remboursements
├── notification/  e-mails et SMS
├── backoffice/    tableaux de bord agent et administrateur
└── shared/        audit, sécurité, événements, configuration
```

Chaque module suit les mêmes couches :
- `api/` : contrôleurs REST et DTO ;
- `web/` : pages Thymeleaf ;
- `application/` : cas d'utilisation et transactions ;
- `domain/` : entités, règles métier et ports (interfaces) ;
- `infrastructure/` : JPA, adaptateurs des fournisseurs et tâches planifiées.

**Principes « objets répartis » appliqués :**
- **Pas de lecture croisée des tables.** Une réservation garde un *instantané* du voyage (`tripId`, libellé, prix) au lieu d'une clé étrangère vers le module `trip`.
- **Inversion de dépendance.** Le module `trip` a besoin des sièges occupés. Il déclare un port `SeatOccupancyPort`, que le module `booking` implémente (`SeatOccupancyAdapter`).
- **Événements de domaine (Observer / Publish-Subscribe).** Les modules communiquent par des événements : `PaymentConfirmed`, `BookingConfirmed`, `BookingCancelled`, `BookingExpired`, `LatePaymentRefundRequested`. Ils jouent le rôle des messages RabbitMQ de la cible microservices.
- **Saga chorégraphiée réservation ↔ paiement, avec ses actions de compensation :**
  - une réservation qui expire annule les paiements en attente ;
  - une annulation déclenche le remboursement ;
  - un paiement arrivé trop tard est remboursé intégralement.
- **API REST** versionnée `/api/v1` en JSON, avec un format d'erreur RFC 7807 (`problem+json`), sécurisée par **JWT**. Elle est prête pour une future application mobile.

### Garantir qu'aucun siège n'est vendu deux fois (RG1, RG2)

- La table `seat_reservation` porte une **contrainte d'unicité (trip_id, seat_number)**. Si deux clients valident au même instant, la base refuse le second. C'est l'équivalent persistant du `SET siege NX EX 900` (Redis) du cahier.
- Le verrou dure **15 minutes**. Une tâche planifiée libère toutes les 30 s les sièges des réservations non payées. Ils sont aussi libérés immédiatement lors d'une nouvelle réservation sur le même voyage.
- Les entités modifiées en concurrence (voyage, réservation, billet, paiement) utilisent le **verrouillage optimiste** (`@Version`).
- Le **paiement est idempotent** : une clé unique est conservée, et un webhook rejoué est ignoré.

### Patrons de conception

| Patron | Où |
|---|---|
| Repository | Spring Data JPA (`*Repository`) |
| DTO | records des contrôleurs `api/` |
| Strategy / Adapter | `PaymentProvider` → `MtnMomoProvider`, `OrangeMoneyProvider`, `CashProvider` |
| Factory | `NotificationFactory` (e-mail, SMS) |
| Observer / Pub-Sub | `DomainEvents` + `@EventListener` / `@TransactionalEventListener` |
| State | `BookingStatus` (transitions autorisées) |
| Saga | réservation ↔ paiement, avec compensations |
| Ports et adaptateurs | `SeatOccupancyPort`, `PaymentProvider`, `NotificationSender` |

### Sécurité

- Mots de passe hachés avec **BCrypt**.
- Contrôle d'accès par rôle (client, agent, administrateur) et par propriété des données :
  - un client ne voit que ses réservations ;
  - un agent ne voit que sa compagnie (**RG7**), et ne peut pas valider le billet d'une autre compagnie.
- **QR code signé en HMAC-SHA256** : un billet falsifié est détecté.
- **Webhook signé en HMAC** : une notification de paiement non signée est refusée (erreur 401).
- Protection **CSRF** sur le site, **JWT de 15 minutes** sur l'API.
- Le montant d'un paiement est toujours calculé côté serveur.
- Aucune donnée de compte Mobile Money n'est stockée.
- **Journal d'audit (RG9)** : annulations, remboursements, changements de tarif, contrôles de billets, faux billets et comptes.

---

## 4. Tests

```bash
./mvnw test
```

La suite compte **24 tests automatisés, tous au vert**.

| Test | Ce qui est vérifié |
|---|---|
| `RefundPolicyTest` | barème RG5 : 80 %, 50 % et 0 % |
| `BookingStatusTest` | transitions du pattern State, montant total, expiration du verrou |
| `QrCodeSignerTest` | QR signé accepté ; QR modifié ou signé avec une autre clé refusé |
| `BookingFlowIntegrationTest` | parcours complet (réservation → paiement → billets → e-mail → contrôle, puis second contrôle refusé) ; siège déjà pris refusé ; **concurrence : 12 clients sur le même siège donnent 1 seul gagnant** ; remboursement de 80 % ; idempotence du paiement et du webhook ; numéro d'un autre opérateur refusé ; RG7 |
| `SeatLockExpirationTest` | RG2 : réservation expirée et sièges libérés |
| `ApiSecurityTest` | recherche publique, JWT obligatoire, inscription puis connexion, webhook non signé refusé |

---

## 5. Base de données PostgreSQL (production)

H2 sert à la démonstration : elle ne demande aucune installation. Le profil `postgres` correspond à la base prévue par le cahier des charges :

```bash
docker run -d --name voyago-db -e POSTGRES_DB=voyago -e POSTGRES_USER=voyago -e POSTGRES_PASSWORD=voyago -p 5432:5432 postgres:16
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Les variables `DB_URL`, `DB_USER` et `DB_PASSWORD` permettent de pointer vers une autre base. En production, les secrets se définissent dans des variables d'environnement : `VOYAGO_JWT_SECRET`, `VOYAGO_QR_SECRET` et `VOYAGO_WEBHOOK_SECRET`.

Pour un **envoi réel des e-mails**, lancez [Mailpit](https://mailpit.axllent.org/) (`docker run -p 1025:1025 -p 8025:8025 axllent/mailpit`) et passez `voyago.mail.enabled` à `true` dans `application.yml`.

---

## 6. Documentation de conception

Les diagrammes UML sont au format PlantUML dans `docs/uml/`, avec leurs images dans `docs/uml/png/` :

| Fichier | Diagrammes |
|---|---|
| `01-cas-utilisation.puml` | cas d'utilisation (UC01 à UC12) |
| `02-classes.puml` | classes du domaine, par module |
| `03-sequences.puml` | 7 séquences : authentification, recherche, verrou des sièges, paiement et webhook, billet et notification, annulation, expiration |
| `04-activites.puml` | réservation de bout en bout, annulation |
| `05-etats.puml` | cycle de vie d'une réservation, cycle de vie d'un paiement |
| `06-composants-deploiement.puml` | composants, déploiement |
| `07-mcd.puml` | modèle physique de données (contraintes d'unicité, index) |

Pour régénérer les images : `java -jar plantuml.jar -Playout=smetana -tpng -o png docs/uml/*.puml`.

---

## 7. Couverture du cahier des charges

| Exigence | Statut |
|---|---|
| F01-F03 Comptes et rôles | ✅ Inscription, connexion, profil, 3 rôles. *Vérification du compte et mot de passe oublié : non faits.* |
| F04-F05 Catalogue | ✅ Compagnies, gares, lignes (administrateur) ; véhicules, voyages et tarifs (agent) |
| F06-F07 Recherche et disponibilités | ✅ Filtres (mode, compagnie, prix, horaire, confort), tri, places restantes |
| F08-F09 Sièges et passagers | ✅ Plan interactif (bus, train, avion), verrou de 15 min, 1 à 6 passagers |
| F10 Annulation et remboursement | ✅ Barème paramétrable (RG5) |
| F11 Report de billet | ❌ Non fait (priorité C) |
| F12-F13 Paiement Mobile Money | ✅ MTN et Orange simulés (sandbox), échecs, nouvelles tentatives, expiration, remboursements, webhook signé |
| F14 Billet PDF avec QR signé | ✅ |
| F15 Contrôle par scan | ✅ Caméra (html5-qrcode) ou saisie manuelle, usage unique |
| F16-F17 E-mails et SMS | ✅ Confirmation, annulation, expiration, rappel de départ. *Mode démo : boîte de réception intégrée ; SMTP réel activable.* |
| F18 Espace client | ✅ Réservations à venir et historique, billets, messages |
| F19 Tableaux de bord | ✅ Agent (sa compagnie) et administrateur (global) |
| F20 Vente au guichet | ✅ Encaissement en espèces |
| F21 Audit et comptes | ✅ Journal d'audit, activation et désactivation des comptes et compagnies |
| F22 Bilingue FR/EN | ❌ Français uniquement (formats locaux XAF et dates en français) |
| F23 PWA | ❌ Non fait (priorité C) |

### Écarts assumés par rapport au cahier

- **Monolithe modulaire** au lieu de 5 microservices : c'est le plan de repli du § 6.2. Chaque module peut devenir un service sans refonte.
- **Contrainte d'unicité en base + tâche planifiée** au lieu de Redis pour les verrous de sièges.
- **Événements Spring internes** au lieu de RabbitMQ.
- **Thymeleaf** (pages générées par le serveur, sans dépendance à Internet) au lieu de React. L'API REST complète reste disponible pour un futur frontend React ou une application mobile.
- **H2** pour la démonstration, **PostgreSQL** prêt avec le profil `postgres`.
- **Ajout du train et de l'avion** au bus, à la demande du groupe.
- Pas encore de CI/CD, de Docker Compose, ni de supervision Prometheus/Grafana.

---

## 8. Technologies

Java 17 · Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator, Mail) · Thymeleaf · H2 / PostgreSQL · JJWT · springdoc-openapi (Swagger UI) · OpenPDF · ZXing · html5-qrcode · JUnit 5, AssertJ, MockMvc.

### Design

Le design actuel (v2) est sobre, avec des boutons compacts. Le premier design (v1) est conservé dans `design-original/` et dans `static/css/app-original.css`. Pour comparer les deux, ajoutez `?design=original` ou `?design=nouveau` à n'importe quelle adresse du site. Le choix est mémorisé dans le navigateur.

### Ressources graphiques (toutes stockées en local : la démo fonctionne sans Internet)

- **Icônes** : [Lucide](https://lucide.dev) v1.57.0 (licence ISC), regroupées dans un sprite SVG `static/img/icons.svg` et appelées par le fragment Thymeleaf `icon('nom')`.
- **Photos** : Unsplash (licence Unsplash, usage libre) dans `static/img/photos/`.
- **Logo** : `static/img/logo.svg`, dessiné pour le projet.
- Détail des crédits : `static/img/CREDITS.txt`.
