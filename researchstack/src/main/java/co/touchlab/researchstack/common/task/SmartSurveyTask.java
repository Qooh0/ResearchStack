package co.touchlab.researchstack.common.task;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.touchlab.researchstack.R;
import co.touchlab.researchstack.common.answerformat.AnswerFormat;
import co.touchlab.researchstack.common.helpers.LogExt;
import co.touchlab.researchstack.common.model.TaskModel;
import co.touchlab.researchstack.common.result.QuestionResult;
import co.touchlab.researchstack.common.result.StepResult;
import co.touchlab.researchstack.common.result.TaskResult;
import co.touchlab.researchstack.common.step.QuestionStep;
import co.touchlab.researchstack.common.step.Step;
import co.touchlab.researchstack.dev.DevUtils;

public class SmartSurveyTask extends Task implements Serializable
{

    private static final String OPERATOR_SKIP = "de";
    private static final String OPERATOR_EQUAL = "eq";
    private static final String OPERATOR_NOT_EQUAL = "ne";
    private static final String OPERATOR_LESS_THAN = "lt";
    private static final String OPERATOR_GREATER_THAN = "gt";
    private static final String OPERATOR_LESS_THAN_EQUAL = "le";
    private static final String OPERATOR_GREATER_THAN_EQUAL = "ge";
    private static final String OPERATOR_OTHER_THAN = "ot";

    private static final String END_OF_SURVEY_MARKER = "END_OF_SURVEY";

    private HashMap<String, Step> steps;
    private HashMap<String, List<TaskModel.RuleModel>> rules;

    private List<String> staticStepIdentifiers;
    private List<String> dynamicStepIdentifiers;

    public SmartSurveyTask(TaskModel taskModel)
    {
        super(taskModel.identifier);
        steps = new HashMap<>(taskModel.elements.size());
        rules = new HashMap<>();
        staticStepIdentifiers = new ArrayList<>(taskModel.elements.size());
        for (TaskModel.StepModel stepModel : taskModel.elements)
        {
            if (stepModel.type.equals("SurveyQuestion"))
            {
                AnswerFormat answerFormat = AnswerFormat.from(stepModel.constraints);

                steps.put(stepModel.identifier,
                        new QuestionStep(stepModel.identifier,
                                stepModel.prompt,
                                answerFormat));
                staticStepIdentifiers.add(stepModel.identifier);
                rules.put(stepModel.identifier,
                        stepModel.constraints.rules);
            }
            else
            {
                DevUtils.throwUnsupportedOpException("Wasn't a survey question");
            }
        }

        dynamicStepIdentifiers = new ArrayList<>(staticStepIdentifiers);
    }

    @Override
    public Step getStepAfterStep(Step step, TaskResult result)
    {
        String currentIdentifier = step == null ? null : step.getIdentifier();
        refillDynamicStepIdentifiers(currentIdentifier);

        String skipToStep = null;

        List<TaskModel.RuleModel> stepRules = rules.get(currentIdentifier);
        if (stepRules != null && stepRules.size() > 0)
        {
            LogExt.d(getClass(),
                    "Rules exist for this step");
            StepResult stepResult = result.getStepResultForStepIdentifier(currentIdentifier);

            // TODO need to make this a lot better, this is what ios does (grabs first result)
            QuestionResult questionResult = (QuestionResult) stepResult.getResults()
                    .values()
                    .toArray()[0];
            if (questionResult != null)
            {
                skipToStep = processRules(stepRules,
                        questionResult.getAnswer());
            }

            if (skipToStep != null && skipToStep.equals(END_OF_SURVEY_MARKER))
            {
                return null;
            }

            if (skipToStep != null)
            {
                adjustDynamicStepIdentifiers(skipToStep,
                        currentIdentifier);
            }
        }
        else
        {
            LogExt.d(getClass(),
                    "No rules for this step");
        }

        String nextStepIdentifier = nextStepIdentifier(true,
                currentIdentifier);

        return nextStepIdentifier == null ? null : steps.get(nextStepIdentifier);
    }

    @Override
    public String getTitleForStep(Context context, Step step)
    {
        int currentIndex = staticStepIdentifiers.indexOf(step.getIdentifier()) + 1;
        return context.getString(R.string.format_step_title, currentIndex, staticStepIdentifiers.size());
    }

    @Override
    public Step getStepBeforeStep(Step step, TaskResult result)
    {
        String currentIdentifier = step == null ? null : step.getIdentifier();
        refillDynamicStepIdentifiers(currentIdentifier);
        String nextStepIdentifier = nextStepIdentifier(false,
                currentIdentifier);
        return nextStepIdentifier == null ? null : steps.get(nextStepIdentifier);
    }

    private String nextStepIdentifier(boolean after, String currentIdentifier)
    {
        if (currentIdentifier == null && after)
        {
            return dynamicStepIdentifiers.size() > 0 ? dynamicStepIdentifiers.get(0) : null;
        }

        int currentIndex = dynamicStepIdentifiers.indexOf(currentIdentifier);
        int newIndex = -1;

        if (after)
        {
            if (currentIndex + 1 < dynamicStepIdentifiers.size())
            {
                newIndex = currentIndex + 1;
            }
        }
        else
        {
            if (currentIndex >= 1)
            {
                newIndex = currentIndex - 1;
            }
        }

        return newIndex != -1 ? dynamicStepIdentifiers.get(newIndex) : null;
    }

    private void refillDynamicStepIdentifiers(String currentIdentifier)
    {
        //Remove till end in dynamic
        int currentIndexInDynamic = dynamicStepIdentifiers.indexOf(currentIdentifier);
        currentIndexInDynamic = currentIndexInDynamic == -1 ? 0 : currentIndexInDynamic;
        dynamicStepIdentifiers = dynamicStepIdentifiers.subList(0, currentIndexInDynamic);

        //Add array from static
        int currentIndexInStatic = staticStepIdentifiers.indexOf(currentIdentifier);
        currentIndexInStatic = currentIndexInStatic == -1 ? 0 : currentIndexInStatic;

        dynamicStepIdentifiers.addAll(staticStepIdentifiers.subList(currentIndexInStatic,
                staticStepIdentifiers.size()));
    }

    private void adjustDynamicStepIdentifiers(String skipToIdentifier, String currentIdentifier)
    {
        int currentIndex = dynamicStepIdentifiers.indexOf(currentIdentifier);
        int skipToIndex = dynamicStepIdentifiers.indexOf(skipToIdentifier);

        if (currentIndex == -1 || skipToIndex == -1)
        {
            return;
        }

        if (skipToIndex > currentIndex)
        {
            while (!dynamicStepIdentifiers.get(currentIndex + 1).equals(skipToIdentifier))
            {
                dynamicStepIdentifiers.remove(currentIndex + 1);
            }
        }
    }

    private String processRules(List<TaskModel.RuleModel> stepRules, Object answer)
    {
        String skipToIdentifier = null;

        // TODO only Integer answers for now
        if (answer == null || answer instanceof Integer)
        {
            for (TaskModel.RuleModel stepRule : stepRules)
            {
                skipToIdentifier = checkRule(stepRule,
                        (Integer) answer);
                if (skipToIdentifier != null)
                {
                    break;
                }
            }
        }
        else
        {
            LogExt.d(getClass(),
                    "Answer is null or not an int");
        }

        return skipToIdentifier;
    }

    private String checkRule(TaskModel.RuleModel stepRule, Integer answer)
    {
        String operator = stepRule.operator;
        String skipTo = stepRule.skipTo;
        int value = stepRule.value;

        if (answer == null)
        {
            return operator.equals(OPERATOR_SKIP) ? skipTo : null;
        }

        int compare = answer.compareTo(value);

        if (operator.equals(OPERATOR_EQUAL))
        {
            return compare == 0 ? skipTo : null;
        }
        else if (operator.equals(OPERATOR_GREATER_THAN))
        {
            return compare > 0 ? skipTo : null;
        }
        else if (operator.equals(OPERATOR_GREATER_THAN_EQUAL))
        {
            return compare >= 0 ? skipTo : null;
        }
        else if (operator.equals(OPERATOR_LESS_THAN))
        {
            return compare < 0 ? skipTo : null;
        }
        else if (operator.equals(OPERATOR_LESS_THAN_EQUAL))
        {
            return compare <= 0 ? skipTo : null;
        }
        else if (operator.equals(OPERATOR_NOT_EQUAL))
        {
            return compare != 0 ? skipTo : null;
        }

        return null;

    }

    @Override
    public Step getStepWithIdentifier(String identifier)
    {
        return steps.get(identifier);
    }

    @Override
    public TaskProgress getProgressOfCurrentStep(Step step, TaskResult result)
    {
        return null;
    }

    @Override
    public void validateParameters()
    {
    }

    @Override
    public int getNumberOfSteps()
    {
        return steps.size();
    }
}
