package org.uteq.backend.service;

import org.springframework.stereotype.Service;
import org.uteq.backend.entity.Convocatoria;

@Service
public class PromptBuilderService {

    public String construir(Convocatoria convocatoria) {
        String titulo      = limpiar(convocatoria.getTitulo());
        String descripcion = limpiar(convocatoria.getDescripcion());
        String contexto    = (titulo + " " + descripcion).toLowerCase();

        AreaTematica area   = detectarArea(contexto);
        String profesional  = detectarProfesional(contexto);

        return String.format(
                "Cinematic wide-format institutional banner for a prestigious university faculty " +
                        "recruitment announcement. Academic field: \"%s\". " +

                        // Estilo visual principal
                        "Art direction: %s. " +

                        // Paleta de colores
                        "Color palette: %s. " +

                        // Elementos simbólicos
                        "Symbolic background elements: %s. " +

                        // Atmósfera
                        "Mood and atmosphere: %s. " +

                        // Escena humana
                        "Main scene: depict %s. " +
                        "The figure should feel aspirational, authentic and deeply engaged in their work — " +
                        "not posing for the camera. Cinematic moment of professional excellence. " +
                        "Diverse representation encouraged. Attire appropriate to the field. " +

                        // Calidad técnica
                        "Technical: ultra-high resolution, 16:9 cinematic landscape format, " +
                        "shallow depth of field with subject in sharp focus and background beautifully blurred, " +
                        "cinematic color grading, dramatic but natural lighting, " +
                        "shot as if by an award-winning editorial photographer, " +
                        "magazine cover quality, hyperrealistic rendering. " +

                        // Restricciones
                        "STRICT: absolutely no text, no letters, no words, no numbers anywhere in the image. " +
                        "No stock photo clichés, no forced smiles, no pointing at whiteboards. " +
                        "Natural, authentic, cinematic storytelling.",

                titulo,
                area.estilo,
                area.paleta,
                area.simbolos,
                area.atmosfera,
                profesional
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE PROFESIONAL / ESCENA HUMANA
    // ══════════════════════════════════════════════════════════════════════

    private String detectarProfesional(String texto) {

        // ── TECNOLOGÍA ───────────────────────────────────────────────────
        if (contiene(texto, "software", "programacion", "desarrollo web", "frontend",
                "backend", "fullstack", "devops", "cloud", "inteligencia artificial",
                "machine learning", "data science", "ciberseguridad", "bases de datos"))
            return "a focused software engineer or data scientist working at multiple curved " +
                    "monitors displaying code and data visualizations, in a dark modern tech lab " +
                    "illuminated by the blue glow of screens, surrounded by whiteboards with " +
                    "diagrams, conveying deep concentration and intellectual mastery";

        if (contiene(texto, "ingenieria", "electronica", "electrica", "mecanica",
                "industrial", "robotica", "automatizacion", "mecatronica", "manufactura"))
            return "a skilled engineer in professional safety gear examining complex industrial " +
                    "machinery or reviewing technical blueprints, standing confidently in a " +
                    "modern factory floor or engineering lab, surrounded by precision instruments " +
                    "and equipment, conveying technical expertise and problem-solving";

        if (contiene(texto, "sistemas", "informatica", "telecomunicaciones", "redes"))
            return "a systems professional or network engineer in a modern server room or " +
                    "operations center, surrounded by racks of glowing equipment and " +
                    "multiple monitoring screens, conveying technical command and precision";

        if (contiene(texto, "arquitectura", "urbanismo", "construccion", "diseño urbano",
                "paisajismo", "habitat"))
            return "an architect deeply studying large-format architectural blueprints or " +
                    "an intricate scale model in a design studio filled with drawings, " +
                    "physical models and natural light, hands active with creative work, " +
                    "conveying vision and spatial intelligence";

        // ── CIENCIAS DE LA SALUD ─────────────────────────────────────────
        if (contiene(texto, "medicina", "medico", "medica", "cirugia", "anatomia",
                "neurologia", "cardiologia", "oncologia", "radiologia", "clinica"))
            return "a physician in a white coat intently examining medical imaging scans " +
                    "or consulting patient data in a modern hospital environment, " +
                    "stethoscope visible, surrounded by advanced medical equipment, " +
                    "conveying expertise, compassion and clinical precision";

        if (contiene(texto, "enfermeria", "salud publica", "fisioterapia",
                "terapia", "rehabilitacion", "odontologia", "optometria"))
            return "a dedicated healthcare professional in clinical attire providing " +
                    "attentive, compassionate care in a bright modern clinical setting, " +
                    "working with specialized equipment, conveying empathy and professional dedication";

        if (contiene(texto, "biologia", "bioquimica", "microbiologia", "genetica",
                "biotecnologia", "laboratorio", "bioinformatica"))
            return "a scientist in a laboratory coat working with an advanced microscope " +
                    "and specialized lab equipment, surrounded by glowing samples, test tubes " +
                    "and scientific instruments in a state-of-the-art research lab, " +
                    "captured in a profound moment of scientific discovery";

        if (contiene(texto, "farmacia", "farmacologia", "quimica farmaceutica"))
            return "a pharmaceutical scientist or pharmacist in a modern lab or pharmacy, " +
                    "carefully working with compounds and precision instruments, " +
                    "surrounded by research materials, conveying scientific rigor and care";

        if (contiene(texto, "veterinaria", "zootecnia", "medicina veterinaria"))
            return "a veterinarian in professional attire examining or caring for an animal " +
                    "in a clinical setting, conveying expertise and genuine care for animal welfare";

        if (contiene(texto, "nutricion", "dietetica", "alimentacion"))
            return "a nutrition professional in a bright clinical or research environment, " +
                    "surrounded by scientific materials and food research tools, " +
                    "conveying health expertise and evidence-based practice";

        // ── CIENCIAS EXACTAS ─────────────────────────────────────────────
        if (contiene(texto, "fisica", "astrofisica", "astronomia"))
            return "a physicist or astrophysicist at a research telescope or advanced physics " +
                    "laboratory, studying complex data on large displays, surrounded by " +
                    "sophisticated scientific instruments, conveying cosmic curiosity and " +
                    "intellectual depth in dramatic lighting";

        if (contiene(texto, "quimica", "fisicoquimica", "quimica analitica"))
            return "a chemist performing an experiment in a modern laboratory, " +
                    "surrounded by colorful reagents and precision glassware, " +
                    "working with focused intensity under specialized lighting, " +
                    "conveying scientific precision and the excitement of discovery";

        if (contiene(texto, "matematica", "estadistica", "calculo", "algebra",
                "probabilidad", "matematica aplicada"))
            return "a mathematician or statistician surrounded by complex equations and " +
                    "data visualizations on large screens or a chalkboard, " +
                    "in a moment of deep intellectual concentration, " +
                    "conveying the beauty and rigor of mathematical thinking";

        if (contiene(texto, "geologia", "geofisica", "geoquimica", "mineria"))
            return "a geologist or earth scientist in the field examining rock formations " +
                    "or studying geological samples in a research environment, " +
                    "surrounded by maps and instruments, conveying scientific fieldwork and discovery";

        if (contiene(texto, "meteorologia", "oceanografia", "ciencias atmosfericas"))
            return "a scientist studying weather or ocean data on large monitoring screens " +
                    "in a research center, surrounded by sophisticated monitoring equipment, " +
                    "conveying environmental science expertise";

        // ── CIENCIAS AMBIENTALES Y AGRONOMÍA ─────────────────────────────
        if (contiene(texto, "ambiental", "medio ambiente", "sostenibilidad",
                "ecologia", "botanica", "zoologia"))
            return "an environmental scientist or ecologist working in the field, " +
                    "taking measurements in a lush natural environment — forest, wetland or " +
                    "coastline — surrounded by scientific equipment, " +
                    "conveying dedication to understanding and protecting the natural world";

        if (contiene(texto, "agronomia", "agricultura", "agropecuaria", "agroecologia",
                "forestal", "silvicultura", "horticultura"))
            return "an agronomist or agricultural engineer in a field or greenhouse, " +
                    "examining crops or conducting soil analysis with specialized tools, " +
                    "under warm natural sunlight, conveying expertise in sustainable food production";

        if (contiene(texto, "energia renovable", "solar", "eolica", "hidrica",
                "eficiencia energetica"))
            return "an energy engineer or environmental specialist inspecting solar panels " +
                    "or wind turbine infrastructure, working confidently in the field, " +
                    "conveying commitment to sustainable energy solutions";

        // ── CIENCIAS SOCIALES Y HUMANIDADES ──────────────────────────────
        if (contiene(texto, "derecho", "juridico", "leyes", "legal", "notariado",
                "constitucional", "penal", "civil", "laboral", "tributario",
                "internacional", "criminologia"))
            return "a lawyer or legal scholar in formal professional attire, studying " +
                    "documents with intense focus in a distinguished law library with " +
                    "floor-to-ceiling bookshelves, conveying authority, intellectual depth " +
                    "and the weight of legal responsibility";

        if (contiene(texto, "psicologia", "psiquiatria", "neurociencia",
                "comportamiento", "cognitivo"))
            return "a psychologist or neuroscientist in a thoughtful consultation setting " +
                    "or research laboratory, surrounded by research materials and instruments, " +
                    "conveying empathy, scientific rigor and deep understanding of the human mind";

        if (contiene(texto, "sociologia", "antropologia", "trabajo social", "ciencias sociales"))
            return "a social scientist or social worker engaged with their community or " +
                    "conducting field research, in an authentic urban or community setting, " +
                    "conveying genuine human connection and professional commitment";

        if (contiene(texto, "comunicacion", "periodismo", "medios", "audiovisual"))
            return "a journalist or communications professional at work — interviewing, " +
                    "reporting or producing content in a dynamic media environment, " +
                    "conveying the urgency and importance of information and storytelling";

        if (contiene(texto, "politica", "relaciones internacionales", "diplomacia",
                "ciencia politica"))
            return "a political scientist or diplomat in a professional conference or " +
                    "research setting, surrounded by documents and international symbols, " +
                    "conveying strategic thinking and global perspective";

        if (contiene(texto, "historia", "filosofia", "literatura", "linguistica",
                "letras", "humanidades"))
            return "a scholar or humanist researcher in a magnificent library or " +
                    "academic study, surrounded by ancient and modern books, " +
                    "in deep contemplation with primary sources, " +
                    "conveying intellectual passion and the depth of humanistic inquiry";

        if (contiene(texto, "geografia", "cartografia", "geoestadistica"))
            return "a geographer or cartographer studying detailed maps and spatial data " +
                    "in a research environment, surrounded by geographic instruments and " +
                    "data visualizations, conveying spatial expertise";

        // ── ECONOMÍA Y NEGOCIOS ──────────────────────────────────────────
        if (contiene(texto, "economia", "macroeconomia", "microeconomia",
                "econometria", "economia internacional"))
            return "an economist analyzing complex economic data and charts on multiple " +
                    "large screens in a research or policy environment, " +
                    "surrounded by reports and data, conveying analytical rigor and " +
                    "the importance of economic decision-making";

        if (contiene(texto, "administracion", "gestion", "negocios", "empresarial",
                "management", "direccion"))
            return "a business leader or manager in a modern corporate environment, " +
                    "confidently leading a strategy session or analyzing business data, " +
                    "in a sleek boardroom or executive office, conveying leadership and vision";

        if (contiene(texto, "finanzas", "contabilidad", "auditoria", "tributacion",
                "banca", "mercado de capitales"))
            return "a financial analyst or accountant in a professional finance environment, " +
                    "studying financial models and data on multiple screens, " +
                    "surrounded by documents and financial instruments, " +
                    "conveying precision and financial expertise";

        if (contiene(texto, "marketing", "publicidad", "branding", "comercial",
                "ventas", "mercadeo"))
            return "a marketing strategist or creative director in a dynamic agency or " +
                    "corporate creative space, surrounded by campaign materials and " +
                    "digital screens with analytics, conveying creativity and strategic thinking";

        if (contiene(texto, "logistica", "supply chain", "comercio", "exportacion",
                "importacion", "aduanas"))
            return "a logistics or supply chain professional in a modern warehouse or " +
                    "operations center, overseeing complex distribution systems, " +
                    "conveying operational mastery and efficiency";

        if (contiene(texto, "emprendimiento", "startup", "innovacion", "incubadora"))
            return "an entrepreneur or innovator in a modern startup environment, " +
                    "working intensely on a new venture surrounded by prototypes and " +
                    "collaborative workspaces, conveying creative energy and entrepreneurial drive";

        if (contiene(texto, "turismo", "hoteleria", "gastronomia", "hospitalidad"))
            return "a hospitality or tourism professional in a premium hotel or " +
                    "culinary environment, showcasing excellence in service and " +
                    "cultural experience, conveying warmth and professional distinction";

        // ── ARTES Y DISEÑO ───────────────────────────────────────────────
        if (contiene(texto, "pintura", "escultura", "grabado", "bellas artes",
                "arte plastico", "ceramica"))
            return "an artist in their studio in the midst of creating a large-scale work, " +
                    "surrounded by canvases, paints, tools and works in progress, " +
                    "captured in a powerful moment of creative expression under dramatic studio lighting";

        if (contiene(texto, "diseño grafico", "diseño visual", "ilustracion",
                "diseño editorial", "tipografia"))
            return "a graphic designer or illustrator working at a large professional " +
                    "display with creative software, surrounded by sketches, color palettes " +
                    "and design references in a modern studio, conveying visual mastery";

        if (contiene(texto, "diseño industrial", "diseño de producto", "diseño de modas",
                "moda", "textil"))
            return "a product or fashion designer working with physical prototypes or " +
                    "materials in a design studio, hands-on with their creation process, " +
                    "surrounded by models, samples and design tools";

        if (contiene(texto, "diseño de interiores", "decoracion", "espacios"))
            return "an interior designer studying architectural plans and material samples " +
                    "in a beautifully designed space, surrounded by swatches, renders " +
                    "and design objects, conveying spatial creativity";

        if (contiene(texto, "fotografia", "cine", "audiovisual", "produccion", "video"))
            return "a filmmaker or photographer in action — behind a professional camera " +
                    "or directing on set, surrounded by professional lighting and production " +
                    "equipment, conveying artistic vision and technical command";

        if (contiene(texto, "musica", "canto", "instrumento", "composicion",
                "orquesta", "jazz", "produccion musical"))
            return "a musician in a powerful performance moment — a pianist absorbed at a " +
                    "grand piano, a conductor leading an orchestra, or a musician in a " +
                    "recording studio — captured with dramatic stage or studio lighting, " +
                    "conveying the transcendent emotion of musical mastery";

        if (contiene(texto, "teatro", "actuacion", "danza", "artes escenicas", "ballet"))
            return "a performing artist in a defining artistic moment — a dancer mid-leap, " +
                    "an actor in intense rehearsal — under dramatic theatrical lighting, " +
                    "conveying the physical and emotional power of performance arts";

        // ── EDUCACIÓN ────────────────────────────────────────────────────
        if (contiene(texto, "educacion inicial", "preescolar", "primera infancia"))
            return "a warm and engaging early childhood educator in a colorful, " +
                    "stimulating classroom environment, surrounded by learning materials, " +
                    "conveying nurturing expertise and passion for early development";

        if (contiene(texto, "educacion", "pedagogia", "docencia", "ensenanza",
                "didactica", "curriculum", "formacion docente"))
            return "a passionate and inspiring university professor at the front of a " +
                    "modern lecture hall or seminar room, engaged in animated teaching " +
                    "with students (shown as silhouettes or from behind), surrounded by " +
                    "books and academic materials, conveying intellectual passion and " +
                    "the transformative power of education";

        if (contiene(texto, "educacion especial", "inclusion", "necesidades educativas"))
            return "a dedicated special education teacher working with focused, " +
                    "individualized attention in a supportive learning environment, " +
                    "surrounded by specialized educational materials, " +
                    "conveying patience, expertise and genuine care";

        // ── DEFECTO — Docente universitario genérico ─────────────────────
        return "a distinguished university professor or academic researcher in a grand " +
                "university library or modern lecture hall, surrounded by books, " +
                "academic materials and the tools of scholarly inquiry, " +
                "deeply engaged with their work in a cinematic moment of intellectual focus, " +
                "conveying authority, passion for knowledge and academic excellence";
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE ÁREA TEMÁTICA (estilo visual, paleta, símbolos, atmósfera)
    // ══════════════════════════════════════════════════════════════════════

    private AreaTematica detectarArea(String texto) {

        // ── TECNOLOGÍA E INGENIERÍA ───────────────────────────────────────
        if (contiene(texto, "software", "programacion", "desarrollo web", "frontend",
                "backend", "fullstack", "devops", "cloud", "inteligencia artificial",
                "machine learning", "data science", "ciberseguridad", "bases de datos"))
            return new AreaTematica(
                    "hyper-futuristic digital lab environment with flowing data streams, " +
                            "holographic interfaces in 3D space, glowing circuit board patterns " +
                            "dissolving into abstract code cascades, cyberpunk-meets-corporate aesthetic",
                    "electric blue #0066FF, deep space black #050A1A, neon cyan #00FFFF, " +
                            "silver metallic #C0C8D0, violet #6600CC",
                    "abstract binary code particles, geometric network nodes connected by light beams, " +
                            "microchip patterns at architectural scale, wireframe spheres, " +
                            "holographic grid perspective, crystalline data structures in background",
                    "cutting-edge innovation, intellectual excitement, digital frontier, " +
                            "the feeling of endless technological possibility"
            );

        if (contiene(texto, "ingenieria", "electronica", "electrica", "mecanica",
                "industrial", "robotica", "automatizacion", "mecatronica", "manufactura"))
            return new AreaTematica(
                    "industrial precision aesthetic merging raw mechanical beauty with " +
                            "elegant engineering, blueprint drafting overlaid on dramatic steel textures, " +
                            "gears and structural elements in photorealistic metal",
                    "industrial steel blue #1B3A6B, chrome silver #8A9BB0, " +
                            "deep charcoal #1A1A2E, warm amber #FF8C00, electric white #F0F4FF",
                    "precision gears in dramatic close-up, structural steel beams as abstract art, " +
                            "blueprint technical drawings floating in 3D space, " +
                            "turbine blades and mechanical components as background elements",
                    "precision, power, human ingenuity at industrial scale, " +
                            "the monumental grandeur of engineering"
            );

        if (contiene(texto, "arquitectura", "urbanismo", "construccion", "diseño urbano"))
            return new AreaTematica(
                    "architectural visualization with dramatic perspective photography, " +
                            "soaring geometric structures, interplay of light and shadow across " +
                            "concrete and glass surfaces",
                    "warm concrete grey #8B8680, sky blue #87CEEB, warm white #FAF7F2, " +
                            "terracotta #C1440E, deep shadow #1C1C1E",
                    "abstract architectural forms with impossible perspectives, " +
                            "staircase spirals and curved facades as abstract art, " +
                            "colonnade shadows creating rhythmic patterns in background",
                    "human scale meeting urban grandeur, the poetry of built space, " +
                            "timeless and contemporary simultaneously"
            );

        if (contiene(texto, "sistemas", "informatica", "telecomunicaciones", "redes"))
            return new AreaTematica(
                    "modern tech operations environment with cool blue lighting, " +
                            "server infrastructure and network topology as visual motifs",
                    "cool grey #2D3748, tech blue #3182CE, white #FFFFFF, " +
                            "accent green #38A169, dark #1A202C",
                    "abstract network topology maps, server rack patterns, " +
                            "data flow visualizations as background elements",
                    "systematic precision, connected world, technological infrastructure"
            );

        // ── CIENCIAS DE LA SALUD ──────────────────────────────────────────
        if (contiene(texto, "medicina", "medico", "medica", "cirugia", "neurologia",
                "cardiologia", "oncologia", "radiologia", "anatomia", "fisiologia"))
            return new AreaTematica(
                    "ultra-clean medical science aesthetic with biophotonic beauty, " +
                            "clinical precision meeting natural wonder, " +
                            "light passing through translucent organic tissue",
                    "clinical white #F8FAFB, healing blue #0077B6, " +
                            "biophotonic green #52B788, soft lavender #9B89C4, silver #E8EDEF",
                    "DNA double helix in crystalline detail, abstract neural network constellations, " +
                            "cellular structures magnified to cosmic scale, molecular bonds as geometric art, " +
                            "microscopic capillary networks as background patterns",
                    "scientific wonder, healing and hope, the profound mystery of life, luminous"
            );

        if (contiene(texto, "biologia", "bioquimica", "microbiologia", "genetica",
                "biotecnologia", "laboratorio", "biofisica"))
            return new AreaTematica(
                    "biopunk science aesthetic where microscopic life becomes breathtaking art, " +
                            "bioluminescent organisms in photorealistic detail",
                    "bioluminescent teal #00B4D8, deep ocean blue #03045E, " +
                            "organic green #52B788, warm amber #F77F00, pure white #FFFFFF",
                    "magnified diatom shells as geometric mandalas, bioluminescent plankton trails, " +
                            "plant cell walls as stained glass, protein folding as abstract sculpture",
                    "astonishing complexity of life, scientific wonder, biophilic and awe-inspiring"
            );

        if (contiene(texto, "enfermeria", "salud publica", "fisioterapia",
                "terapia", "rehabilitacion"))
            return new AreaTematica(
                    "warm healthcare aesthetic balancing clinical precision with human compassion, " +
                            "soft organic forms suggesting care and wellness",
                    "warm teal #2A9D8F, soft coral #F4A261, clinical white #FAFAFA, " +
                            "calming sage #87A878, gentle lavender #B8B8FF",
                    "abstract healing symbols, organic flowing forms suggesting vitality, " +
                            "soft botanical elements blending with medical geometry",
                    "warmth, compassion, hope, professional dedication to human wellbeing"
            );

        if (contiene(texto, "veterinaria", "zootecnia"))
            return new AreaTematica(
                    "natural science aesthetic combining clinical precision with the living world, " +
                            "warm earthy tones with scientific elements",
                    "earth green #4A7C59, warm brown #8B5E3C, " +
                            "clean white #FAFAFA, sky blue #87CEEB, gold #CFB53B",
                    "abstract animal forms reduced to elegant line art, " +
                            "biological diagrams as decorative elements, natural textures in background",
                    "care for living beings, scientific expertise, the bond between humans and animals"
            );

        if (contiene(texto, "agronomia", "agricultura", "forestal", "ambiental",
                "ecologia", "medio ambiente", "sostenibilidad"))
            return new AreaTematica(
                    "biophilic nature-meets-science aesthetic, macro photography of natural " +
                            "textures as abstract art, the sublime beauty of ecosystems",
                    "forest deep green #1A4301, earth brown #6B4226, " +
                            "sky blue #87CEEB, sun yellow #F9C74F, pure white #FAFAFA",
                    "aerial forest canopy abstracted to fractal patterns, " +
                            "cross-section of tree rings as mandala, leaf venation networks as golden ratio spirals, " +
                            "water ripple interference in background",
                    "reverence for nature, sustainability and hope, grounded and inspiring"
            );

        // ── CIENCIAS EXACTAS ──────────────────────────────────────────────
        if (contiene(texto, "fisica", "astrofisica", "astronomia"))
            return new AreaTematica(
                    "cosmic science aesthetic merging the mathematical sublime with visual beauty, " +
                            "the hidden geometry of the universe revealed",
                    "deep space black #020818, nebula purple #6B2FA0, " +
                            "star white #F0F0FF, quantum blue #0096FF, supernova gold #FFD700",
                    "Mandelbrot fractal landscapes at cosmic scale, particle collision trails " +
                            "forming floral patterns, gravitational lensing as abstract art, " +
                            "geometric platonic solids floating in space",
                    "cosmic awe, intellectual transcendence, the beauty of pure physics, sublime"
            );

        if (contiene(texto, "quimica", "bioquimica", "quimica analitica", "farmacia"))
            return new AreaTematica(
                    "laboratory precision aesthetic with colorful chemical beauty, " +
                            "molecular structures as art forms",
                    "lab white #F8F9FA, chemistry blue #4361EE, " +
                            "reagent amber #F77F00, glass silver #ADB5BD, deep navy #03045E",
                    "molecular bond structures as 3D sculpture, " +
                            "periodic table elements abstracted to geometric patterns, " +
                            "crystal lattice structures, chemical reaction visualizations in background",
                    "scientific precision, the beauty of molecular structure, " +
                            "the excitement of chemical discovery"
            );

        if (contiene(texto, "matematica", "estadistica", "calculo", "algebra"))
            return new AreaTematica(
                    "mathematical sublime aesthetic where equations become visual landscapes, " +
                            "the hidden geometry of pure mathematics revealed as art",
                    "deep space black #020818, nebula purple #6B2FA0, " +
                            "electric blue #0096FF, gold #FFD700, pure white #F0F0FF",
                    "fractal geometry patterns, abstract mathematical surfaces rendered as terrain, " +
                            "golden ratio spirals, topological forms, geometric proofs as visual art",
                    "intellectual transcendence, the beauty of pure mathematics, " +
                            "the joy of abstract reasoning"
            );

        // ── CIENCIAS SOCIALES Y HUMANIDADES ──────────────────────────────
        if (contiene(texto, "derecho", "juridico", "leyes", "legal", "notariado",
                "constitucional", "penal", "civil", "criminologia"))
            return new AreaTematica(
                    "neoclassical gravitas with monumental legal symbolism, " +
                            "marble and bronze textures conveying institutional permanence, " +
                            "dramatic chiaroscuro lighting on architectural elements",
                    "Carrara marble white #F5F5F0, deep burgundy #722F37, " +
                            "aged bronze #CD7F32, charcoal #2D2D2D, gold leaf #CFB53B",
                    "scales of justice abstracted to pure geometric balance, " +
                            "classical columns and porticos as abstract forms, " +
                            "parchment and seal textures layered dramatically in background",
                    "gravitas, institutional authority, the timeless pursuit of justice, solemn"
            );

        if (contiene(texto, "psicologia", "psiquiatria", "neurociencia", "cognitivo"))
            return new AreaTematica(
                    "surrealist-meets-neuroscience aesthetic exploring the landscape of the mind, " +
                            "abstract representations of consciousness",
                    "deep indigo #3D2B6E, soft periwinkle #9896CF, " +
                            "warm rose #E8A598, silver mist #D4D8E2, golden #F0C040",
                    "abstract neural networks forming constellations, " +
                            "brain topography as mountainous landscape, " +
                            "labyrinthine patterns suggesting introspection in background",
                    "the mysterious depth of human consciousness, empathy and scientific curiosity"
            );

        if (contiene(texto, "sociologia", "antropologia", "trabajo social",
                "comunicacion", "periodismo", "politica"))
            return new AreaTematica(
                    "contemporary editorial design with bold graphic impact, " +
                            "abstract representations of human connection and social networks",
                    "bold coral #FF6B6B, deep teal #264653, warm sand #E9C46A, " +
                            "slate #457B9D, clean white #F1FAEE",
                    "abstract human network as constellation of connected nodes, " +
                            "world map abstracted to flowing data visualization, " +
                            "social graph patterns as artistic composition in background",
                    "human connection, social progress, the complexity of society, forward-looking"
            );

        if (contiene(texto, "historia", "filosofia", "literatura", "linguistica",
                "letras", "humanidades"))
            return new AreaTematica(
                    "refined academic aesthetic with warm classical tones, " +
                            "the beauty of scholarship and intellectual tradition",
                    "warm parchment #F5E6C8, deep burgundy #6B2D3E, " +
                            "aged gold #C9A84C, rich brown #5C3317, ivory #FFFFF0",
                    "ancient manuscripts and maps abstracted as texture, " +
                            "classical typography forms as decorative elements, " +
                            "library architecture and book spines in background",
                    "intellectual depth, the richness of human culture, " +
                            "reverence for knowledge across centuries"
            );

        // ── ECONOMÍA Y NEGOCIOS ───────────────────────────────────────────
        if (contiene(texto, "economia", "finanzas", "contabilidad", "auditoria",
                "administracion", "negocios", "empresarial", "gestion", "management"))
            return new AreaTematica(
                    "premium corporate financial aesthetic with the visual language of global business, " +
                            "sleek and authoritative, conveying stability and ambition",
                    "corporate navy #1B2A4A, gold #C9A84C, " +
                            "platinum #E5E5E5, deep charcoal #2C2C2C, success green #2ECC71",
                    "abstract skyscraper skylines as geometric silhouettes, " +
                            "upward trend lines forming architectural compositions, " +
                            "global network connections as luminous web in background",
                    "ambition, stability, global vision, premium and trustworthy"
            );

        if (contiene(texto, "marketing", "publicidad", "branding", "comercial", "mercadeo"))
            return new AreaTematica(
                    "dynamic creative-meets-commercial aesthetic, bold and energetic, " +
                            "the visual language of brand building",
                    "vibrant coral #FF6B35, deep navy #1B2A4A, " +
                            "clean white #FFFFFF, electric yellow #FFD700, slate #6C757D",
                    "abstract brand identity shapes and patterns, " +
                            "dynamic composition suggesting movement and impact, " +
                            "creative studio visual elements in background",
                    "creativity, impact, the power of ideas and visual communication"
            );

        if (contiene(texto, "emprendimiento", "startup", "innovacion"))
            return new AreaTematica(
                    "modern startup aesthetic combining raw energy with sophisticated design, " +
                            "the visual language of disruption and innovation",
                    "vibrant orange #FF6B35, deep purple #6B2FA0, " +
                            "white #FFFFFF, electric blue #0066FF, gold #FFD700",
                    "abstract lightbulb and rocket forms, network growth patterns, " +
                            "dynamic geometric compositions suggesting upward movement",
                    "entrepreneurial energy, innovation, the excitement of building something new"
            );

        // ── ARTES Y DISEÑO ────────────────────────────────────────────────
        if (contiene(texto, "arte", "bellas artes", "pintura", "escultura",
                "ilustracion", "grabado", "ceramica"))
            return new AreaTematica(
                    "painterly expressionist aesthetic where the banner itself becomes fine art, " +
                            "rich impasto textures, dramatic brushwork",
                    "rich vermillion #DC143C, cadmium yellow #FFD700, " +
                            "ultramarine blue #4169E1, raw umber #8B6914, canvas cream #F5F0E8",
                    "abstract paint strokes of monumental scale, palette knife textures, " +
                            "color field gradients, abstract sculpture forms in background",
                    "raw creative energy, artistic freedom, the joy of making, bold and expressive"
            );

        if (contiene(texto, "diseño", "ui", "ux", "branding", "grafico", "visual"))
            return new AreaTematica(
                    "ultra-modern Swiss design school aesthetic, geometric precision meets creativity, " +
                            "the discipline of good design as pure form",
                    "primary red #E63946, deep black #1A1A1A, " +
                            "pure white #FFFFFF, accent yellow #FFB703, cool grey #6C757D",
                    "Bauhaus-inspired geometric compositions, abstract grid systems as visual art, " +
                            "negative space as active compositional element in background",
                    "intellectual rigor, creative discipline, the beauty of purposeful form"
            );

        if (contiene(texto, "musica", "canto", "instrumento", "composicion", "orquesta"))
            return new AreaTematica(
                    "synesthetic visualization of music as pure light and form, " +
                            "sound waves as sweeping architectural curves",
                    "deep concert black #0D0D0D, vibrant gold #FFD700, " +
                            "rich crimson #9B0000, silver #C8C8C8, spotlight amber #FF9500",
                    "sound wave interference patterns forming mandalas, " +
                            "musical staff lines as flowing ribbons of light, " +
                            "abstract instrument forms and acoustic patterns in background",
                    "the transcendent power of music, dramatic and emotional, solemn beauty"
            );

        if (contiene(texto, "teatro", "danza", "artes escenicas", "actuacion"))
            return new AreaTematica(
                    "theatrical dramatic aesthetic with powerful stage lighting, " +
                            "the raw emotion of live performance",
                    "deep stage black #0A0A0A, spotlight gold #FFD700, " +
                            "dramatic red #9B1B1B, silver #C0C0C0, warm amber #FF9500",
                    "abstract stage lighting beams, curtain fabric textures, " +
                            "motion blur suggesting dynamic movement in background",
                    "the emotional power of performance, drama and human expression"
            );

        // ── EDUCACIÓN ─────────────────────────────────────────────────────
        if (contiene(texto, "educacion", "pedagogia", "docencia", "ensenanza",
                "didactica", "curriculum", "formacion"))
            return new AreaTematica(
                    "warm and inspiring educational aesthetic celebrating knowledge and growth, " +
                            "organic forms suggesting learning and development",
                    "warm amber #F4A261, sky blue #4CC9F0, " +
                            "leaf green #52B788, warm white #FFFBF0, earthy brown #8B5E3C",
                    "open book pages fanning into abstract wings, abstract tree of knowledge, " +
                            "light rays breaking through architectural forms, " +
                            "geometric educational symbols in background",
                    "the joy of discovery, the transformative power of education, hopeful"
            );

        // ── DEFECTO — Institucional UTEQ ──────────────────────────────────
        return new AreaTematica(
                "prestigious Latin American university aesthetic combining classical academic " +
                        "tradition with contemporary institutional design, monumental and inspiring",
                "UTEQ emerald green #00A63E, deep forest #016630, " +
                        "gold #CFB53B, pure white #FFFFFF, warm cream #FDF8F0",
                "abstract university portico and colonnade forms, " +
                        "graduation cap abstracted to geometric diamond, " +
                        "academic laurel wreath as circular geometric pattern, " +
                        "architectural dome silhouette in background",
                "academic prestige, institutional excellence, the pursuit of knowledge, " +
                        "inspiring and authoritative, celebrating academic tradition"
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private boolean contiene(String texto, String... palabras) {
        for (String p : palabras)
            if (texto.contains(p)) return true;
        return false;
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.replaceAll("[\"'\\\\]", "").trim();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CLASE INTERNA
    // ══════════════════════════════════════════════════════════════════════

    private static class AreaTematica {
        final String estilo;
        final String paleta;
        final String simbolos;
        final String atmosfera;

        AreaTematica(String estilo, String paleta, String simbolos, String atmosfera) {
            this.estilo    = estilo;
            this.paleta    = paleta;
            this.simbolos  = simbolos;
            this.atmosfera = atmosfera;
            //fix
        }
    }
}