"""Generate REPORT.pdf containing only the Q&A answers from the README.

Spec Part 3 of the submission brief says:
  "The report must be prepared in a PDF format, which must only include the
   answers to the questions in each part."
This script produces that PDF from a single source of truth (the Q&A text
kept in sync with README.md).
"""
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.enums import TA_JUSTIFY
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak
)
from reportlab.lib import colors

OUTPUT = "REPORT.pdf"

TITLE = "5COSC022W &mdash; Smart Campus Sensor &amp; Room Management API"
SUBTITLE = "Conceptual Report (question answers)"
AUTHOR = "Aqeel Larif &mdash; University of Westminster"

# (section-heading, answer-text)   --- keep in sync with README.md
ANSWERS = [
    ("Part 1.1 &mdash; JAX-RS resource lifecycle &amp; concurrency",
     """By default, JAX-RS treats a root resource class as <b>per-request</b>: a fresh
     instance is constructed for every incoming HTTP request, so instance fields
     cannot be assumed to survive across requests. Any state that must persist (the
     <font face="Courier">rooms</font>, <font face="Courier">sensors</font>, and
     <font face="Courier">readings</font> maps in this API) therefore has to live
     <i>outside</i> the resource &mdash; here it lives in the
     <font face="Courier">DataStore</font> enum singleton, which exists for the
     lifetime of the JVM. Because Grizzly dispatches requests on a worker-thread
     pool, that singleton is accessed concurrently; I used
     <font face="Courier">ConcurrentHashMap</font> for the top-level collections
     and <font face="Courier">Collections.synchronizedList</font> for the per-sensor
     reading history so that map reads, map writes, and list appends are all safe
     without coarse-grained <font face="Courier">synchronized</font> blocks. A plain
     <font face="Courier">HashMap</font> risks losing entries or throwing
     <font face="Courier">ConcurrentModificationException</font> during a concurrent
     re-hash; caching data on resource fields would effectively throw it away every
     request."""),

    ("Part 1.2 &mdash; Why HATEOAS matters",
     """The <font face="Courier">/api/v1</font> discovery endpoint returns not only
     metadata but a <font face="Courier">resources</font> map whose values are
     <b>absolute URIs</b> to the top-level collections, with supported verbs and
     nested sub-resource links. This is the HATEOAS idea: the server, not the
     client, is the source of truth for <i>what actions are possible and where</i>.
     A client that started at <font face="Courier">/api/v1</font> and followed
     links never needs to hard-code a URL pattern; if the API later moves
     <font face="Courier">sensors</font> to a new path or publishes a new resource,
     the client picks it up automatically. Compared to static documentation,
     hypermedia (1)&nbsp;couples the client to URI <i>roles</i> instead of URI
     <i>shapes</i>, (2)&nbsp;lets the server progressively enable/disable
     capabilities at runtime, and (3)&nbsp;makes the API discoverable from the
     root, which is how browsers have scaled the human web."""),

    ("Part 2.1 &mdash; IDs vs full objects in list responses",
     """<font face="Courier">GET /rooms</font> currently returns full
     <font face="Courier">Room</font> objects. Returning only IDs would shrink each
     response dramatically and save bandwidth when the caller just needs to page
     through identifiers (e.g. an autocomplete). The cost is that every client that
     actually needs <font face="Courier">name</font> or
     <font face="Courier">capacity</font> must make a follow-up
     <font face="Courier">GET /rooms/{id}</font> for every single entry &mdash; the
     classic &ldquo;N+1 request&rdquo; problem, which is usually slower overall
     than one larger payload. The pragmatic middle ground is a thin summary DTO
     (<font face="Courier">id</font>, <font face="Courier">name</font>,
     <font face="Courier">capacity</font>) on the collection endpoint and the full
     object on the detail endpoint; since the coursework&rsquo;s
     <font face="Courier">Room</font> already is small, returning the full objects
     is a reasonable default."""),

    ("Part 2.2 &mdash; Is DELETE idempotent here?",
     """Yes. Idempotency is a statement about <i>resulting state</i>, not the
     response code. First <font face="Courier">DELETE /rooms/LAB-101</font> returns
     <b>204</b> and removes the room; a second identical call returns <b>404</b>
     (the room is already gone), but the state of the server is unchanged &mdash;
     it is still &ldquo;no LAB-101&rdquo;. Because the end state is the same no
     matter how many times the call is made, the operation satisfies RFC&nbsp;7231&rsquo;s
     definition of idempotent. Note this is distinct from &ldquo;safe&rdquo;:
     <font face="Courier">DELETE</font> does mutate state, so it is idempotent but
     not safe."""),

    ("Part 3.1 &mdash; @Consumes(APPLICATION_JSON) mismatch",
     """<font face="Courier">@Consumes</font> is used during <b>content
     negotiation</b>, before the resource method is invoked. When a client sends
     <font face="Courier">Content-Type: text/plain</font> to an endpoint that only
     declares <font face="Courier">@Consumes(MediaType.APPLICATION_JSON)</font>,
     Jersey&rsquo;s request matcher cannot find a method able to accept that body,
     so the framework short-circuits and returns <b>415 Unsupported Media
     Type</b>. The method body never runs, which is exactly what we want &mdash;
     <font face="Courier">MessageBodyReader&lt;Sensor&gt;</font> for JSON would
     never have been able to parse a plain-text payload, and letting it try would
     surface an internal parsing error to the client instead of a well-defined
     protocol-level response."""),

    ("Part 3.2 &mdash; @QueryParam vs path for filtering",
     """A query parameter (<font face="Courier">/sensors?type=CO2</font>) expresses
     <i>&ldquo;the same collection, narrowed by a criterion&rdquo;</i>. A path
     segment (<font face="Courier">/sensors/type/CO2</font>) would assert that
     <font face="Courier">CO2</font> is a distinct sub-resource, which it
     isn&rsquo;t &mdash; it&rsquo;s the same collection filtered differently.
     Query parameters also compose cleanly
     (<font face="Courier">?type=CO2&amp;status=ACTIVE&amp;limit=50</font>) while
     path segments force a combinatorial explosion of routes. Caching behaviour is
     also clearer: intermediaries treat <font face="Courier">?type=CO2</font> as a
     variant of the parent resource rather than as a separate entity, which
     matches the real semantics."""),

    ("Part 4.1 &mdash; Benefits of the sub-resource locator pattern",
     """<font face="Courier">SensorResource.readings(...)</font> is a
     <b>sub-resource locator</b>: it has no HTTP verb annotation, it only resolves
     the path prefix and returns a
     <font face="Courier">SensorReadingResource</font> instance. That handoff
     gives us two concrete benefits. First, separation of concerns &mdash;
     <font face="Courier">SensorResource</font> does not need to know anything
     about reading history, and
     <font face="Courier">SensorReadingResource</font> only knows about one
     concrete sensor (it receives it in its constructor, so every method inside
     has trivial access to <font face="Courier">parentSensor</font>). Second,
     composability &mdash; when tomorrow we add filtering or aggregation on
     readings, the new <font face="Courier">@GET</font> / <font face="Courier">@POST</font>
     methods simply live in <font face="Courier">SensorReadingResource</font>
     instead of bloating <font face="Courier">SensorResource</font> with
     <font face="Courier">sensors/{id}/readings/...</font> endpoints. The
     alternative &mdash; a single monolithic controller handling every path level
     &mdash; scales linearly with complexity and becomes unreadable quickly."""),

    ("Part 5.2 &mdash; Why 422, not 404",
     """A <b>404</b> means &ldquo;the URL you asked for does not name anything in
     my universe.&rdquo; That is not what happened when the client POSTs
     <font face="Courier">{"roomId":"NOPE"}</font> to
     <font face="Courier">/sensors</font>: the URL <font face="Courier">/sensors</font>
     is valid, the method is valid, and the JSON parses correctly &mdash; the body
     just references a room that does not exist. <b>422 Unprocessable Entity</b>
     was introduced by WebDAV and reused by modern APIs for exactly this scenario:
     syntactically valid, semantically invalid. Returning 404 would confuse the
     client into thinking <font face="Courier">/sensors</font> itself is gone,
     which would break retry logic or monitoring that watches for missing
     endpoints. 422 keeps the diagnosis inside the payload, where the field and
     value pinpoint the exact problem."""),

    ("Part 5.4 &mdash; Cybersecurity risk of leaked stack traces",
     """<font face="Courier">GenericExceptionMapper</font> logs the full
     <font face="Courier">Throwable</font> to the server but returns a bland JSON
     body to the client. Returning the raw trace instead would hand attackers a
     reconnaissance goldmine: the exact Jersey version (useful for CVE matching),
     the JDK build, internal package names that reveal project layout, file paths
     that hint at the deployment environment, and sometimes snippets of query text
     or sanitized SQL. An attacker with that information can target known
     vulnerabilities, phrase social-engineering attacks more convincingly, or
     probe more efficiently for misconfigurations. The OWASP &ldquo;Improper Error
     Handling&rdquo; category exists because of exactly this failure mode."""),

    ("Part 5.5 &mdash; Why filters for cross-cutting concerns",
     """<font face="Courier">LoggingFilter</font> is one class that logs every
     request and every response for the entire API. If the same observability
     were expressed as <font face="Courier">Logger.info(...)</font> calls inside
     each resource method, you would need to remember to add them to every new
     endpoint, the log format would drift across contributors, and refactoring
     would touch dozens of files to change one field. Filters make logging
     <b>pluggable</b> &mdash; annotate with <font face="Courier">@Provider</font>,
     register once, and every request/response flows through it, including error
     responses produced by exception mappers. The same mechanism scales to
     authentication, request-id propagation, metrics, and CORS, which is why
     JAX-RS ships filters as the supported extension point for cross-cutting
     behaviour."""),
]


def build():
    styles = getSampleStyleSheet()

    title = ParagraphStyle(
        "Title", parent=styles["Title"],
        fontName="Helvetica-Bold", fontSize=18, leading=22,
        spaceAfter=6, textColor=colors.HexColor("#1f3a68"))
    subtitle = ParagraphStyle(
        "Subtitle", parent=styles["Normal"],
        fontName="Helvetica-Oblique", fontSize=12, leading=16,
        spaceAfter=4, alignment=1, textColor=colors.HexColor("#555555"))
    author = ParagraphStyle(
        "Author", parent=styles["Normal"],
        fontName="Helvetica", fontSize=11, leading=14,
        spaceAfter=18, alignment=1, textColor=colors.HexColor("#333333"))
    h = ParagraphStyle(
        "Heading", parent=styles["Heading2"],
        fontName="Helvetica-Bold", fontSize=13, leading=16,
        spaceBefore=14, spaceAfter=6,
        textColor=colors.HexColor("#1f3a68"))
    body = ParagraphStyle(
        "Body", parent=styles["BodyText"],
        fontName="Helvetica", fontSize=11, leading=15,
        alignment=TA_JUSTIFY, spaceAfter=6)

    doc = SimpleDocTemplate(
        OUTPUT, pagesize=A4,
        leftMargin=2.2 * cm, rightMargin=2.2 * cm,
        topMargin=2.2 * cm,  bottomMargin=2.2 * cm,
        title="Smart Campus API - Conceptual Report",
        author="Aqeel Larif")

    flow = [
        Paragraph(TITLE, title),
        Paragraph(SUBTITLE, subtitle),
        Paragraph(AUTHOR, author),
        Spacer(1, 0.2 * cm),
    ]
    for heading, text in ANSWERS:
        flow.append(Paragraph(heading, h))
        flow.append(Paragraph(text, body))

    doc.build(flow)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
