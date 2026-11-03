package com.example.adenmedicaltest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adenmedicaltest.ui.theme.EmeraldSuccess
import com.example.adenmedicaltest.ui.theme.IndigoDark
import com.example.adenmedicaltest.ui.theme.IndigoPrimary
import com.example.adenmedicaltest.ui.theme.Slate200

@Composable
fun AboutPrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_privacy_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "معلومات التطبيق وسياسة الخصوصية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aden Medical Test — الإصدار 1.0.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_about_back_top")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رجوع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // App Hero Overview Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(IndigoDark, IndigoPrimary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎓", fontSize = 26.sp)
                            }
                            Column {
                                Text(
                                    text = "بوابة اختبار القبول الطبي",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "كلية الطب والعلوم الصحية — جامعة عدن",
                                    color = Color(0xFFC7D2FE),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "تطبيق تعليمي وتدريبي متكامل مصمم لمساعدة الطلاب المتقدمين لاختبارات القبول والمفاضلة في الكليات الطبية بجامعة عدن، عبر محاكاة دقيقة لبيئة الاختبار الفعلية مع التصحيح الفوري والتفسير العلمي المعتمد.",
                            color = Color(0xFFE0E7FF),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "1,142 سؤالاً علمياً",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "يدعم العمل دون إنترنت",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Exam Mechanism & Educational Features
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "آلية الاختبار والمميزات التعليمية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureBullet(
                        icon = Icons.Default.CheckCircle,
                        title = "تصنيف شامل للمواد المقررة",
                        description = "يضم بنك الأسئلة 179 سؤالاً في الأحياء، و525 سؤالاً في الكيمياء، و438 سؤالاً في اللغة الإنجليزية، مقسمة إلى مستويات متدرجة واختبارات عشوائية."
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)
                    FeatureBullet(
                        icon = Icons.Default.Info,
                        title = "تصحيح فوري وتفسير علمي مختصر",
                        description = "فور اختيار الإجابة، يظهر الخيار الصحيح وحالة الإجابة (صحيحة أو خاطئة) مع تعليل علمي مختصر ومحايد يشرح سبب صحة الإجابة."
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)
                    FeatureBullet(
                        icon = Icons.Default.Lock,
                        title = "اعتماد الإجابة ومنع التغيير",
                        description = "بمجرد اختيار الإجابة وإظهار التصحيح الفوري، يتم قفل السؤال واعتماد إجابته لضمان دقة التقييم ومحاكاة معايير الاختبار."
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)
                    FeatureBullet(
                        icon = Icons.Default.CloudOff,
                        title = "العمل دون اتصال وحفظ تلقائي للنتائج",
                        description = "جميع الأسئلة والتفسيرات مخزنة محلياً داخل التطبيق، كما يتم حفظ تقدم الطالب ونتائجه محلياً على الجهاز لمنع ضياعها عند انقطاع الاتصال أو إغلاق التطبيق."
                    )
                }
            }
        }

        // Privacy Policy Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_policy_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "سياسة الخصوصية وحماية البيانات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PolicySection(
                        number = "1",
                        title = "البيانات التي يتعامل معها التطبيق",
                        body = "يحترم تطبيق Aden Medical Test خصوصية الطلاب بشكل كامل. عند استخدام الوضع المحلي دون اتصال، لا يطلب التطبيق أي بيانات شخصية وتُحفظ نتائج الاختبارات محلياً على جهازك فقط. وعند اختيار تسجيل الدخول عبر حساب Google، يقتصر الاستخدام على معرف الحساب والاسم والبريد الإلكتروني لمزامنة سجل نتائجك عبر السحابة."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PolicySection(
                        number = "2",
                        title = "أمن البيانات وعدم المشاركة مع أطراف ثالثة",
                        body = "تُخزن بيانات النتائج السحابية في قواعد بيانات Firebase Firestore المؤمنة بقواعد حماية صارمة تضمن ألا يطلع على سجل الطالب إلا الطالب نفسه. لا يقوم التطبيق ببيع أو تأجير أو مشاركة أي بيانات شخصية مع أي جهات إعلانية أو أطراف ثالثة."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PolicySection(
                        number = "3",
                        title = "الصلاحيات المطلوبة في النظام",
                        body = "يطلب التطبيق صلاحية الاتصال بالإنترنت (INTERNET و ACCESS_NETWORK_STATE) فقط للمزامنة السحابية الاختيارية وخدمات الشرح الذكي، ولا يطلب أي صلاحيات حساسة مثل الكاميرا أو الموقع الجغرافي أو جهات الاتصال أو الملفات الشخصية."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PolicySection(
                        number = "4",
                        title = "حق المستخدم في التحكم وحذف البيانات",
                        body = "يمتلك الطالب التحكم الكامل في سجل نتائجه، حيث يمكنه حذف أي نتيجة اختبار من شاشة سجل النتائج في أي وقت، أو تسجيل الخروج والاعتماد على التخزين المحلي."
                    )
                }
            }
        }

        // Technical Metadata Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "محتوى تعليمي محايد ومراجع علمياً",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "v1.0.0 (Build 1)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_about_back_bottom")
            ) {
                Text("العودة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IndigoPrimary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PolicySection(
    number: String,
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$number. $title",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
