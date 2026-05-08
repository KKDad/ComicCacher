import Link from 'next/link';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-6">
      <Card className="max-w-md w-full">
        <CardHeader>
          <CardTitle>Page not found</CardTitle>
          <CardDescription>
            We couldn&apos;t find what you were looking for. It may have moved, or never existed.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link href="/">Back to comics</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
