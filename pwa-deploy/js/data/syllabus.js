/* University of Aden admission syllabus 2026/2027: faculties, tracks, subjects and units. */
window.Aden = window.Aden || {};
Aden.syllabus = [
    {
        id: 'medical',
        title: 'الكليات الطبية وكلية التمريض',
        subtitle: 'الطب والعلوم الصحية، طب الأسنان، الصيدلة، والتمريض',
        icon: 'stethoscope',
        color: 'emerald',
        badge: 'المسار الطبي (1,140 سؤالاً)',
        tracks: [
            {
                id: 'medical_main',
                name: 'المسار الطبي الموحد',
                subjects: [
                    {
                        id: 'chem_med',
                        name: 'الكيمياء (المنهج الثانوي اليمني)',
                        lang: 'ar',
                        units: [
                            { id: 'cm1', grade: 'أول ثانوي', title: 'الوحدة السابعة: التفاعلات الكيميائية والمعادلات الموزونة', pages: 'ص 131 - 154' },
                            { id: 'cm2', grade: 'أول ثانوي', title: 'الوحدة الثامنة: الكيمياء العضوية', pages: 'ص 155 - 188' },
                            { id: 'cm3', grade: 'ثاني ثانوي', title: 'الوحدة السادسة: سرعة التفاعلات والاتزان الكيميائي', pages: 'ص 127 - 166' },
                            { id: 'cm4', grade: 'ثاني ثانوي', title: 'الوحدة السابعة: الاتزان الأيوني في المحاليل المائية', pages: 'ص 167 - 196' },
                            { id: 'cm5', grade: 'ثاني ثانوي', title: 'الوحدة التاسعة: الهيدروكربونات الأروماتية', pages: 'ص 221 - 246' },
                            { id: 'cm6', grade: 'ثاني ثانوي', title: 'الوحدة العاشرة: المركبات العضوية الأكسجينية', pages: 'ص 247 - 294' },
                            { id: 'cm7', grade: 'ثالث ثانوي', title: 'الوحدة الثالثة: الطاقة الكهربائية وتفاعلات الأكسدة والاختزال', pages: 'ص 57 - 95' },
                            { id: 'cm8', grade: 'ثالث ثانوي', title: 'الوحدة الخامسة: مركبات النيتروجين العضوية', pages: 'ص 65 - 95 (ج2)' },
                            { id: 'cm9', grade: 'ثالث ثانوي', title: 'الوحدة السادسة: الكيمياء الحيوية', pages: 'ص 97 - 117 (ج2)' }
                        ]
                    },
                    {
                        id: 'bio_med',
                        name: 'الأحياء (المنهج الثانوي اليمني)',
                        lang: 'ar',
                        units: [
                            { id: 'bm1', grade: 'أول ثانوي', title: 'تركيب الخلية الحيوانية ووظائف مكوناتها', pages: 'منهج 1 ث' },
                            { id: 'bm2', grade: 'أول ثانوي', title: 'التغذية في الإنسان (الجهاز الهضمي، اضطرابات تناول الغذاء)', pages: 'منهج 1 ث' },
                            { id: 'bm3', grade: 'أول ثانوي', title: 'النقل في الإنسان (الجهاز الدوري، الدم، الدورة الدموية واللمفاوية)', pages: 'منهج 1 ث' },
                            { id: 'bm4', grade: 'أول ثانوي', title: 'التنفس والإخراج في الإنسان (الجهاز التنفسي والبولي وأمراضهما)', pages: 'منهج 1 ث' },
                            { id: 'bm5', grade: 'ثاني ثانوي', title: 'انقسام الخلية (دورة الخلية، المتساوي، الأورام، الاختزالي)', pages: 'منهج 2 ث' },
                            { id: 'bm6', grade: 'ثاني ثانوي', title: 'إنتاج الطاقة (التحلل السكري، دورة كربس، سلسلة نقل الإلكترون)', pages: 'منهج 2 ث' },
                            { id: 'bm7', grade: 'ثاني ثانوي', title: 'المناعة (جهاز المناعة، اللقاحات والأمصال، المناعة الطبيعية والمكتسبة)', pages: 'منهج 2 ث' },
                            { id: 'bm8', grade: 'ثالث ثانوي', title: 'الجهاز العصبي وأعضاء الحس والتنظيم الهرموني في الإنسان', pages: 'منهج 3 ث' },
                            { id: 'bm9', grade: 'ثالث ثانوي', title: 'التكاثر في الإنسان وتكوين الجنين وتغذيته', pages: 'منهج 3 ث' },
                            { id: 'bm10', grade: 'ثالث ثانوي', title: 'الوراثة المندلية وغير المندلية والوراثة الجزيئية (DNA & RNA)', pages: 'منهج 3 ث' }
                        ]
                    },
                    {
                        id: 'eng_med',
                        name: 'اللغة الإنجليزية الطبية (Faculty of Medicine Syllabus)',
                        lang: 'en',
                        units: [
                            { id: 'em1', grade: 'Grammar', title: 'Tenses, Modals, Active/Passive Voice, Conditionals, Direct/Indirect', pages: 'Crescent 4-6' },
                            { id: 'em2', grade: 'Grammar', title: 'Prepositions, Quantifiers, Comparatives/Superlatives, Articles', pages: 'Crescent 4-6' },
                            { id: 'em3', grade: 'Functions', title: 'Describing substances/processes/life cycles, Classifying, Defining', pages: 'Science Reader' },
                            { id: 'em4', grade: 'Lexis', title: 'Medical Terms, Chemical Symbols, Abbreviations & Scientific Lexis', pages: 'Medical Syllabus' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'sports_nutrition',
        title: 'كلية التربية الرياضية والتغذية العلاجية',
        subtitle: 'تخصص التغذية العلاجية / العلاج الطبيعي',
        icon: 'heart-pulse',
        color: 'teal',
        badge: 'تغذية وعلاج طبيعي (1,000 سؤال)',
        tracks: [
            {
                id: 'nutrition_main',
                name: 'مسار التغذية العلاجية والعلاج الطبيعي',
                subjects: [
                    {
                        id: 'bio_nut',
                        name: 'مادة الأحياء',
                        lang: 'ar',
                        units: [
                            { id: 'bn1', grade: 'أول ثانوي', title: 'الوحدة الثالثة (التغذية): التغذية الذاتية، البناء الضوئي، الهضم والامتصاص', pages: 'ص 65 - 110' },
                            { id: 'bn2', grade: 'أول ثانوي', title: 'الوحدة الخامسة (التنفس): مراحل التنفس في الإنسان وبعض أمراض الجهاز التنفسي', pages: 'ص 156 - 165' },
                            { id: 'bn3', grade: 'ثاني ثانوي', title: 'الوحدة الخامسة (المناعة): المناعة، جهاز المناعة، الأمصال واللقاحات، الإيدز', pages: 'ص 139 - 154' },
                            { id: 'bn4', grade: 'ثالث ثانوي', title: 'الوحدة الأولى (الجهاز العصبي): الألياف العصبية، أجزاء الجهاز العصبي، أعضاء الحس', pages: 'ص 9 - 45' },
                            { id: 'bn5', grade: 'ثالث ثانوي', title: 'الوحدة الثانية (التنظيم الهرموني): التنظيم الهرموني في الإنسان والغدد الصماء', pages: 'ص 57 - 65' }
                        ]
                    },
                    {
                        id: 'chem_nut',
                        name: 'مادة الكيمياء (كتاب الثالث ثانوي فقط)',
                        lang: 'ar',
                        units: [
                            { id: 'cn1', grade: 'ثالث ثانوي', title: 'الوحدة الأولى: العناصر الانتقالية (الخواص، التركيب الإلكتروني، الحديد)', pages: 'ص 5 - 30' },
                            { id: 'cn2', grade: 'ثالث ثانوي', title: 'الوحدة الثانية: الطاقة الحرارية (صور الطاقة، السعة الحرارية، قانون هس)', pages: 'ص 31 - 56' },
                            { id: 'cn3', grade: 'ثالث ثانوي', title: 'الوحدة الثالثة: الطاقة الكهربائية وتفاعلات الأكسدة والاختزال', pages: 'ص 57 - 95' },
                            { id: 'cn4', grade: 'ثالث ثانوي', title: 'الوحدة الرابعة: الطاقة والتفاعلات النووية (طاقة الترابط، النشاط الإشعاعي)', pages: 'ص 5 - 64 (ج2)' },
                            { id: 'cn5', grade: 'ثالث ثانوي', title: 'الوحدة الخامسة: مركبات النيتروجين العضوية (الأمينات، الأميدات)', pages: 'ص 65 - 95 (ج2)' },
                            { id: 'cn6', grade: 'ثالث ثانوي', title: 'الوحدة السادسة: الكيمياء الحيوية (الكربوهيدرات، البروتينات، الدهون، الفيتامينات)', pages: 'ص 97 - 117 (ج2)' }
                        ]
                    },
                    {
                        id: 'eng_nut',
                        name: 'مادة اللغة الإنجليزية',
                        lang: 'en',
                        units: [
                            { id: 'en1', grade: 'Literary Sec.', title: 'Crescent Course Book 4, 5, 6 (Grammar & Language Functions)', pages: '1st, 2nd & 3rd Sec.' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'law',
        title: 'كلية الحقوق',
        subtitle: 'الدراسات القانونية والشرعية والدستورية',
        icon: 'scale',
        color: 'amber',
        badge: 'المسار القانوني (1,000 سؤال)',
        tracks: [
            {
                id: 'law_main',
                name: 'مسار كلية الحقوق',
                subjects: [
                    {
                        id: 'arabic_law',
                        name: 'مادة اللغة العربية',
                        lang: 'ar',
                        units: [
                            { id: 'al1', grade: 'الأدب والنصوص', title: 'العصور الأدبية، الصعاليك، عمرو بن كلثوم (1ث) | المتنبي، الموشحات (2ث) | الكلاسيكية، الرومانسية، شوقي، المقالة (3ث)', pages: '1 + 2 + 3 ثانوي' },
                            { id: 'al2', grade: 'البلاغة والنقد', title: 'أساليب الإنشاء الطلبي (2ث) | المقالة وأنواعها وعناصرها (3ث)', pages: '2 + 3 ثانوي' },
                            { id: 'al3', grade: 'النحو والصرف', title: 'الأسماء الخمسة، المثنى والجمع، الممنوع من الصرف، التصغير، النسب، الإعلال والإبدال، الكشف في المعاجم', pages: '1 + 2 + 3 ثانوي' },
                            { id: 'al4', grade: 'القراءة', title: 'آداب السلوك، العربية صورة وجودنا، المرأة في الإسلام (1ث) | القرآن واللغة، مأرب، القاضي العادل (2ث) | صنعاء، الحرية، العولمة (3ث)', pages: '1 + 2 + 3 ثانوي' }
                        ]
                    },
                    {
                        id: 'const_law',
                        name: 'دستور الجمهورية اليمنية',
                        lang: 'ar',
                        units: [
                            { id: 'cl1', grade: 'الدستور اليمني', title: 'باب ومواد الحقوق والواجبات الأساسية للمواطنين حصراً', pages: 'المواد 41 - 61' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'cs_it',
        title: 'كلية الحاسوب وتكنولوجيا المعلومات',
        subtitle: 'علوم الحاسوب، تقنية المعلومات، وهندسة البرمجيات',
        icon: 'cpu',
        color: 'blue',
        badge: 'تقني وحاسوبي (1,378 سؤالاً)',
        tracks: [
            {
                id: 'cs_main',
                name: 'مسار الحاسوب وتكنولوجيا المعلومات',
                subjects: [
                    {
                        id: 'math_cs',
                        name: 'مادة الرياضيات',
                        lang: 'ar',
                        units: [
                            { id: 'mcs1', grade: 'أول ثانوي (ج1)', title: 'المنطق الرياضي (العبارات المنطقية، أدوات الربط، جداول الصواب، التكافؤ)', pages: '1 ث' },
                            { id: 'mcs2', grade: 'ثاني ثانوي (ج2)', title: 'المصفوفات والمحددات وعملياتها', pages: '2 ث علمي' },
                            { id: 'mcs3', grade: 'ثاني وثالث علمي', title: 'الاحتمالات والتفاضل والتكامل', pages: '2 و 3 ث علمي' }
                        ]
                    },
                    {
                        id: 'cs_core',
                        name: 'مادة الحاسوب',
                        lang: 'ar',
                        units: [
                            { id: 'csc1', grade: 'أول ثانوي', title: 'الوحدة 1: الحاسوب وأهميته (درس التطور التاريخي محذوف)', pages: 'منهج 1 ث' },
                            { id: 'csc2', grade: 'أول ثانوي', title: 'الوحدة 2: المكونات المادية (Hardware) | الوحدة 3: البرمجيات (Software)', pages: 'منهج 1 ث' },
                            { id: 'csc3', grade: 'ثاني ثانوي', title: 'الوحدة 1: تقنية المعلومات والاتصالات | الوحدة 2: أنظمة العد | الوحدة 3: الإنترنت', pages: 'منهج 2 ث' }
                        ]
                    },
                    {
                        id: 'eng_cs',
                        name: 'مادة اللغة الإنجليزية',
                        lang: 'en',
                        units: [
                            { id: 'ecs1', grade: 'Pupil Book 6', title: 'Units 1-6: Describing things, Reporting Events, Looking for a Job, Tables & Flow Charts', pages: '3rd Sec. + WB' },
                            { id: 'ecs2', grade: 'Readers', title: 'Arts Reader + Science Reader', pages: '3rd Sec.' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'economics_politics',
        title: 'كلية الاقتصاد والعلوم السياسية',
        subtitle: 'تخصصات الاقتصاد، العلوم المالية والمصرفية، والعلوم السياسية',
        icon: 'trending-up',
        color: 'indigo',
        badge: 'مساران تخصصيان (1,000 سؤال)',
        tracks: [
            {
                id: 'econ_finance',
                name: 'المسار الأول: اقتصاد / علوم مالية ومصرفية',
                subjects: [
                    {
                        id: 'math_econ',
                        name: 'مادة الرياضيات (14 موضوعاً محدداً)',
                        lang: 'ar',
                        units: [
                            { id: 'me1', grade: 'الإحصاء والارتباط', title: '1. مقاييس النزعة المركزية | 2. مقاييس التشتت | 3. الارتباط | 4. الانحدار الخطي', pages: 'موضوعات 1-4' },
                            { id: 'me2', grade: 'الجبر والاحتمالات', title: '5. التباديل | 6. التوافيق | 7. مفكوك ذات الحدين | 8. الاحتمالات', pages: 'موضوعات 5-8' },
                            { id: 'me3', grade: 'التحليل والمتتاليات', title: '9. التفاضل (مشتقة أولى وثانية) | 10. التكامل | 11. المتتاليات الحسابية | 12. الهندسية', pages: 'موضوعات 9-12' },
                            { id: 'me4', grade: 'المصفوفات', title: '13. المصفوفات (جمع وطرح وضرب) | 14. المحددات وحل معادلتين خطيتين (كرامر)', pages: 'موضوعات 13-14' }
                        ]
                    },
                    {
                        id: 'prin_econ',
                        name: 'مادة مبادئ الاقتصاد (ثاني ثانوي أدبي)',
                        lang: 'ar',
                        units: [
                            { id: 'pe1', grade: 'الوحدات 1 - 3', title: 'الوحدة 1: الفكر وعلم الاقتصاد | الوحدة 2: المشكلة الاقتصادية | الوحدة 3: عناصر الإنتاج', pages: '2 ث أدبي' },
                            { id: 'pe2', grade: 'الوحدات 4 - 5', title: 'الوحدة 4: الطلب وعرض السوق وتوازن السوق | الوحدة 5: النقود والبنوك', pages: '2 ث أدبي' }
                        ]
                    }
                ]
            },
            {
                id: 'pol_science',
                name: 'المسار الثاني: تخصص العلوم السياسية',
                subjects: [
                    {
                        id: 'prin_econ_pol',
                        name: 'مادة مبادئ الاقتصاد (الوحدات الخمس)',
                        lang: 'ar',
                        units: [
                            { id: 'pep1', grade: 'ثاني ثانوي أدبي', title: 'الفكر الاقتصادي، المشكلة الاقتصادية، عناصر الإنتاج، العرض والطلب، النقود والبنوك', pages: 'الوحدات 1 - 5 كاملة' }
                        ]
                    },
                    {
                        id: 'pol_culture',
                        name: 'مادة الثقافة السياسية (تاريخ وجغرافيا 3 ثانوي)',
                        lang: 'ar',
                        units: [
                            { id: 'pc1', grade: 'التاريخ المعاصر', title: 'تاريخ العالم الحديث والمعاصر (3ث): الثورات الكبرى، الحروب الكبرى، الهيئات والمنظمات، المعالم التاريخية', pages: 'منهج 3 ث أدبي' },
                            { id: 'pc2', grade: 'الجغرافيا السياسية', title: 'جغرافية العالم المعاصر (3ث): الجغرافيا السياسية، التكتلات الاقتصادية، قضايا معاصرة، مشكلات عالمية', pages: 'منهج 3 ث أدبي' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'languages',
        title: 'كلية اللغات والترجمة',
        subtitle: 'بكالوريوس اللغة الإنجليزية التجارية وبكالوريوس الترجمة',
        icon: 'languages',
        color: 'purple',
        badge: 'مساران لغويان (1,000 سؤال)',
        tracks: [
            {
                id: 'bus_english',
                name: 'المسار الأول: بكالوريوس اللغة الإنجليزية التجارية',
                subjects: [
                    {
                        id: 'eng_bus',
                        name: 'Business English Grammar, Functions & Lexis',
                        lang: 'en',
                        units: [
                            { id: 'eb1', grade: 'Grammar', title: 'Tenses, Passive, Conditionals, Questions, Conjunctions, Relative Clauses, Modals, Adjectives', pages: 'Crescent 4, 5, 6' },
                            { id: 'eb2', grade: 'Functions', title: 'Describing, Classifying, Instructing, Asking/Giving directions, Looking for a job, Advice', pages: 'Crescent 4, 5, 6' },
                            { id: 'eb3', grade: 'Business Lexis', title: 'Business Terminology and Commercial Texts', pages: 'Specialized Lexis' }
                        ]
                    }
                ]
            },
            {
                id: 'translation',
                name: 'المسار الثاني: بكالوريوس الترجمة (إنجليزي - عربي)',
                subjects: [
                    {
                        id: 'eng_trans',
                        name: 'Translation, Idioms, Proverbs & Advanced Grammar',
                        lang: 'en',
                        units: [
                            { id: 'et1', grade: 'Core Syllabus', title: 'All Business English Grammar & Language Functions', pages: 'Crescent 4, 5, 6' },
                            { id: 'et2', grade: 'Added Functions', title: 'Suggesting, Inviting & Making Polite Requests', pages: 'Translation Track' },
                            { id: 'et3', grade: 'Translation', title: 'Translating Idioms and Proverbs from English into Arabic', pages: 'Idiomatic Translation' }
                        ]
                    }
                ]
            }
        ]
    },
    {
        id: 'engineering',
        title: 'كلية الهندسة',
        subtitle: 'جميع الأقسام الهندسية (الرياضيات، الفيزياء، الإنجليزية التقنية)',
        icon: 'compass',
        color: 'rose',
        badge: 'المسار الهندسي (1,136 سؤالاً)',
        tracks: [
            {
                id: 'eng_main',
                name: 'المسار الهندسي الموحد',
                subjects: [
                    {
                        id: 'math_eng',
                        name: 'مادة الرياضيات الهندسية',
                        lang: 'ar',
                        units: [
                            { id: 'men1', grade: 'حساب المثلثات', title: 'النسب والعلاقات، مجموع وفرق زاويتين، ضعف ونصف الزاوية، تحويل المجموع إلى ضرب', pages: 'المرحلة الثانوية' },
                            { id: 'men2', grade: 'الهندسة التحليلية', title: 'ميل المستقيم ومعادلاته، الدائرة والمماس، القطوع المخروطية (مكافئ، ناقص، زائد)', pages: 'المرحلة الثانوية' },
                            { id: 'men3', grade: 'الجبر والأعداد المركبة', title: 'الأعداد المركبة (العمليات، الصورة المثلثية، دي موافر) + التباديل والتوافيق وذات الحدين', pages: 'ثالث ثانوي علمي' },
                            { id: 'men4', grade: 'التفاضل والتكامل', title: 'النهايات والاتصال، الاشتقاق الضمني والمثلثي واللوغاريتمي، التكامل بالتعويض والتجزئة والمساحات والحجوم', pages: 'ثالث ثانوي علمي' }
                        ]
                    },
                    {
                        id: 'phys_eng',
                        name: 'مادة الفيزياء',
                        lang: 'ar',
                        units: [
                            { id: 'pe_1', grade: 'أول ثانوي', title: 'الوحدة 3: خواص المواد الصلبة والموائع | الوحدة 4: الشغل والقدرة والطاقة', pages: 'منهج 1 ث' },
                            { id: 'pe_2', grade: 'ثاني ثانوي', title: 'الوحدة 1: اتزان الأجسام | الوحدة 8: التيار المستمر | الوحدة 9: المغناطيسية', pages: 'منهج 2 ث' },
                            { id: 'pe_3', grade: 'ثالث ثانوي', title: 'الوحدة 2: التيار المتردد | الوحدة 3: الإلكترونيات | الوحدة 5: الفيزياء الذرية | الوحدة 6: الإشعاع والأطياف', pages: 'منهج 3 ث' }
                        ]
                    },
                    {
                        id: 'eng_tech',
                        name: 'اللغة الإنجليزية التقنية (Technical English)',
                        lang: 'en',
                        units: [
                            { id: 'etech1', grade: 'Crescent 6', title: 'Crescent Book 6 (Units 1-5 with Language Reviews) + Grammar', pages: 'Book 6 & WB 6' },
                            { id: 'etech2', grade: 'Technical Lexis', title: 'Science Reader (Book 5 & 6) + General Technical Terminologies', pages: '2nd & 3rd Sec.' }
                        ]
                    }
                ]
            }
        ]
    }
];
