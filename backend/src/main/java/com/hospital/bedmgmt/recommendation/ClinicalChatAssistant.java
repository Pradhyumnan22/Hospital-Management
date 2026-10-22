package com.hospital.bedmgmt.recommendation;

import com.hospital.bedmgmt.domain.WardType;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RankedBed;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationCriteria;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationResult;

import java.util.List;
import java.util.Locale;

/**
 * Turns a back-and-forth with staff into bed constraints, and only ranks a bed once the request is clear.
 */
public final class ClinicalChatAssistant {

    public ParsedRequest read(String message) {
        return interpret(message);
    }

    public Turn respond(String patientName, List<String> history, String message, RecommendationResult result) {
        String transcript = transcript(history, message);
        ParsedRequest parsed = interpret(transcript);
        String latest = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        if (isGreeting(latest) && !hasClinicalDetail(transcript)) {
            return new Turn(parsed, "Hi. What's going on with " + patientName + "? A stomach ache, chest pain, trouble breathing, anything like that is enough.");
        }
        if (parsed.criteria().requiredWardType() == null) {
            return new Turn(parsed, "Tell me the problem in plain words. A stomach ache, a fever, chest pain, or a pregnancy all send me looking in different places.");
        }
        if (!parsed.ready()) {
            String ward = label(parsed.criteria().requiredWardType());
            return new Turn(parsed, "Okay, " + ward + " for " + patientName + ". Do they need oxygen, a ventilator, or a monitor? If a normal bed is fine, just say so.");
        }
        if (result.candidates().isEmpty()) {
            return new Turn(parsed, "I looked, and nothing open fits that for " + patientName + ". We can loosen something, like dropping the ventilator, or try another ward. What do you want to change?");
        }
        RankedBed top = result.candidates().get(0);
        String reply = parsed.understood() + " I'd put " + patientName + " in " + top.bedCode() + " on " + top.wardName() + ".";
        if (result.candidates().size() > 1) {
            RankedBed next = result.candidates().get(1);
            reply += " If that room is awkward, " + next.bedCode() + " is the next one I'd use.";
        }
        reply += " Want me to hold " + top.bedCode() + "? Hit confirm and I'll allocate it.";
        return new Turn(parsed, reply);
    }

    public boolean ready(String transcript) {
        return interpret(transcript).ready();
    }

    private ParsedRequest interpret(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        WardType ward = ward(lower);
        boolean isolation = contains(lower, "isolation") && !negated(lower, "isolation");
        boolean ventilator = (contains(lower, "ventilat") || contains(lower, "vent ")) && !negated(lower, "ventilat");
        boolean monitor = contains(lower, "monitor") && !negated(lower, "monitor");
        boolean oxygen = (contains(lower, "oxygen") || contains(lower, "o2")) && !negated(lower, "oxygen") && !negated(lower, "o2");
        String understood = "";
        boolean fromProblem = false;
        if (any(lower, "chest pain", "heart attack", "cardiac", "stroke", "seizure", "unconscious", "unresponsive", "short of breath", "shortness of breath", "can't breathe", "cannot breathe", "trouble breathing", "pneumonia", "sepsis")) {
            ward = ward == null ? WardType.ICU : ward;
            monitor = true;
            oxygen = true;
            fromProblem = true;
            understood = "That sounds serious, so I'm looking for a monitored bed with oxygen.";
        } else if (any(lower, "covid", "tuberculosis", "measles", "chickenpox", "contagious", "infectious")) {
            ward = WardType.ISOLATION;
            isolation = true;
            fromProblem = true;
            understood = "That needs an isolation bed so they aren't on an open ward.";
        } else if (any(lower, "pregnan", "labour", "labor", "contraction", "childbirth", "delivery", "maternity")) {
            ward = ward == null ? WardType.MATERNITY : ward;
            fromProblem = true;
            understood = "That sounds like a maternity stay.";
        } else if (any(lower, "child", "kid", "infant", "toddler", "newborn", "baby")) {
            ward = ward == null ? WardType.PEDIATRIC : ward;
            fromProblem = true;
            understood = "I'll look on the pediatric ward.";
        } else if (any(lower, "stomach", "stomachache", "abdominal", "belly", "nausea", "vomit", "headache", "fever", "cough", "diarrhea", "diarrhoea", "sore throat", "sprain", "fracture", "broken", "dizzy", "pain", "hip", "injury", "injured", "walk", "limp", "swelling", "wound", "cut")) {
            ward = ward == null ? WardType.GENERAL : ward;
            fromProblem = true;
            understood = "A problem like that usually needs a general bed, not intensive care.";
        } else if (ward == null && lower.trim().length() > 12 && !isGreeting(lower.trim())) {
            ward = WardType.GENERAL;
            fromProblem = true;
            understood = "I don't have a special ward for that, so I'm treating it as a general admission.";
        }
        if (ward == WardType.ISOLATION) {
            isolation = true;
        }
        if (understood.isBlank() && ward != null) {
            understood = "I'll look for a " + label(ward) + " bed.";
        }
        boolean ready = ward != null && (fromProblem || equipmentMentioned(lower));
        return new ParsedRequest(new RecommendationCriteria(ward, ward, isolation, ventilator, monitor, oxygen), understood, ready);
    }

    private boolean equipmentMentioned(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("oxygen") || lower.contains("o2") || lower.contains("ventilat") || lower.contains("vent")
                || lower.contains("monitor") || lower.contains("isolation")
                || lower.contains("normal") || lower.contains("standard") || lower.contains("nothing else")
                || lower.contains("no equipment") || lower.contains("that's all") || lower.contains("thats all");
    }

    private boolean hasClinicalDetail(String text) {
        return ward(text.toLowerCase(Locale.ROOT)) != null || equipmentMentioned(text);
    }

    private boolean isGreeting(String latest) {
        return latest.equals("hi") || latest.equals("hello") || latest.equals("hey") || latest.startsWith("hi ") || latest.startsWith("hello ") || latest.startsWith("hey ");
    }

    private boolean contains(String text, String word) {
        return text.contains(word);
    }

    private boolean any(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean negated(String text, String word) {
        return text.contains("no " + word) || text.contains("without " + word) || text.contains("not " + word) || text.contains("don't need " + word) || text.contains("dont need " + word);
    }

    private WardType ward(String text) {
        if (text.contains("icu") || text.contains("intensive")) {
            return WardType.ICU;
        }
        if (text.contains("maternity") || text.contains("obstetric")) {
            return WardType.MATERNITY;
        }
        if (text.contains("paed") || text.contains("pediatr")) {
            return WardType.PEDIATRIC;
        }
        if (text.contains("isolation")) {
            return WardType.ISOLATION;
        }
        if (text.contains("general")) {
            return WardType.GENERAL;
        }
        return null;
    }

    private String label(WardType ward) {
        return ward.name().toLowerCase(Locale.ROOT);
    }

    private String transcript(List<String> history, String message) {
        StringBuilder combined = new StringBuilder();
        if (history != null) {
            for (String turn : history) {
                if (turn != null && !turn.isBlank()) {
                    combined.append(turn).append('\n');
                }
            }
        }
        if (message != null) {
            combined.append(message);
        }
        return combined.toString();
    }

    public record ParsedRequest(RecommendationCriteria criteria, String understood, boolean ready) {
    }

    public record Turn(ParsedRequest parsed, String reply) {
    }
}
