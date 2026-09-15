import type { Gender } from "@/data/gender";

export interface CreateAthleteInput {
  name: string;
  gender: Gender;
  clubId: number | null;
}

export type UpdateAthleteInput = CreateAthleteInput;

export interface Athlete {
  id: number;
  name: string;
  gender: Gender;
  clubId: number | null;
  createdAt: string;
}
