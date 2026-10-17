// Module: quality:detekt-rules (detekt 1.23.x API). Sensor S-05.
// Pattern for project-specific rules: the message tells the agent HOW to fix.
// Register in META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider.
package com.example.app.quality.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "project-rules"
    override fun instance(config: Config) = RuleSet(ruleSetId, listOf(SensitiveDataInLogRule(config)))
}

/**
 * Flags logger calls whose arguments reference identifiers that usually hold card data or PII.
 * Constitution Art. V (PCI DSS / LGPD), anti-pattern AP-07.
 */
class SensitiveDataInLogRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SensitiveDataInLog",
        severity = Severity.Security,
        description = "Dados de cartão ou PII não podem ser enviados para logs.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val receiver = (expression.parent as? KtDotQualifiedExpression)?.receiverExpression?.text ?: return
        if (receiver !in LOGGERS) return

        val leaked = expression.valueArguments
            .flatMap { it.collectDescendantsOfType<KtNameReferenceExpression>() }
            .map { it.getReferencedName() }
            .firstOrNull { name -> SENSITIVE.any { name.contains(it, ignoreCase = true) } }
            ?: return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "'$leaked' parece conter dado sensível e está sendo logado. " +
                    "Como corrigir: remova o valor da mensagem e registre apenas o tipo do evento " +
                    "ou um ID de correlação. Ver docs/harness/guides/security.md",
            ),
        )
    }

    private companion object {
        val LOGGERS = setOf("Timber", "logger", "Logger", "log", "AppLogger")
        val SENSITIVE = listOf("pan", "cvv", "cardNumber", "track", "pin", "cpf", "cnpj", "password", "token", "email", "phone")
    }
}
