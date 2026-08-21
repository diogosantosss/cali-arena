import type { Gender } from "@/types/gender";

export interface CreateAthleteInput {
  name: string;
  gender: Gender;
  clubId: number;
}

export interface Athlete {
  id: number;
  name: string;
  gender: Gender;
  clubId: number;
  createdAt: string;
}
