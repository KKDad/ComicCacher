import { CardDescription, CardHeader } from '@/components/ui/card';

interface AuthHeaderProps {
  title: string;
  description: string;
}

export function AuthHeader({ title, description }: AuthHeaderProps) {
  return (
    <CardHeader className="space-y-1 text-center">
      <p className="font-display text-3xl font-bold text-primary mb-4">Comics Hub</p>
      <h1 className="font-sans text-2xl font-semibold leading-none">{title}</h1>
      <CardDescription>{description}</CardDescription>
    </CardHeader>
  );
}
