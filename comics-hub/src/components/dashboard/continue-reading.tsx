'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookOpen } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';

export function ContinueReading() {
  // TODO: Replace with actual data from GraphQL
  const isLoading = false;
  const lastRead = null;

  if (isLoading) {
    return (
      <section>
        <h2 className="text-xl font-semibold mb-4 text-ink">
          Continue Where You Left Off
        </h2>
        <Card className="max-w-sm">
          <CardContent className="p-6">
            <Skeleton className="h-48 w-full mb-4 rounded-lg" />
            <Skeleton className="h-6 w-3/4 mb-2" />
            <Skeleton className="h-4 w-1/2" />
          </CardContent>
        </Card>
      </section>
    );
  }

  if (!lastRead) {
    return (
      <section>
        <h2 className="text-xl font-semibold mb-4 text-ink">
          Continue Where You Left Off
        </h2>
        <Card className="max-w-sm border-dashed">
          <CardContent className="flex flex-col items-center justify-center p-12 text-center">
            <BookOpen className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No recent reading history</p>
            <p className="text-sm text-ink-muted mb-4">
              Start reading to see your progress here
            </p>
            <Button>Browse Comics</Button>
          </CardContent>
        </Card>
      </section>
    );
  }

  return (
    <section>
      <h2 className="text-xl font-semibold mb-4 text-ink">
        Continue Where You Left Off
      </h2>
      <Card className="max-w-sm hover:shadow-md transition-shadow">
        <CardContent className="p-6">
          <div className="aspect-[3/4] bg-canvas rounded-lg mb-4 overflow-hidden">
            {/* TODO: Replace with actual image */}
            <div className="w-full h-full flex items-center justify-center text-ink-muted">
              Comic Image
            </div>
          </div>
          <CardTitle className="text-lg mb-1">Comic Title</CardTitle>
          <CardDescription>Last read: 2 days ago</CardDescription>
          <Button className="w-full mt-4">Continue Reading</Button>
        </CardContent>
      </Card>
    </section>
  );
}
